/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2014 ForgeRock AS
 */

package com.forgerock.opendj.ldap.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.ResultHandler;
import org.forgerock.opendj.ldap.requests.AddRequest;
import org.forgerock.opendj.ldap.requests.DeleteRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldif.EntryGenerator;
import org.forgerock.util.promise.Promise;

import com.forgerock.opendj.cli.ArgumentException;
import com.forgerock.opendj.cli.ArgumentParser;
import com.forgerock.opendj.cli.BooleanArgument;
import com.forgerock.opendj.cli.CommonArguments;
import com.forgerock.opendj.cli.ConnectionFactoryProvider;
import com.forgerock.opendj.cli.ConsoleApplication;
import com.forgerock.opendj.cli.IntegerArgument;
import com.forgerock.opendj.cli.MultiChoiceArgument;
import com.forgerock.opendj.cli.StringArgument;

import static com.forgerock.opendj.cli.ArgumentConstants.*;
import static com.forgerock.opendj.cli.Utils.*;
import static com.forgerock.opendj.ldap.tools.ToolsMessages.*;

/**
 * A load generation tool that can be used to load a Directory Server with Add
 * and Delete requests using one or more LDAP connections.
 */
public class AddRate extends ConsoleApplication {

    private static final class AddPerformanceRunner extends PerformanceRunner {
        private final class AddStatsHandler extends UpdateStatsResultHandler<Result> {
            private final String entryDN;

            private AddStatsHandler(final long currentTime, String entryDN) {
                super(currentTime);
                this.entryDN = entryDN;
            }

            @Override
            public void handleResult(final Result result) {
                super.handleResult(result);

                switch (delStrategy) {
                case RANDOM:
                    long newKey;
                    do {
                        newKey = randomSeq.get().nextInt();
                    } while (dnEntriesAdded.putIfAbsent(newKey, this.entryDN) != null);
                    break;
                case FIFO:
                    long uniqueTime = currentTime;
                    while (dnEntriesAdded.putIfAbsent(uniqueTime, this.entryDN) != null) {
                        uniqueTime++;
                    }
                    break;
                default:
                    break;
                }

                nbAdd.getAndIncrement();
            }
        }

        private final class DeleteStatsHandler extends UpdateStatsResultHandler<Result> {

            private DeleteStatsHandler(final long startTime) {
                super(startTime);
            }

            @Override
            public void handleResult(final Result result) {
                super.handleResult(result);
                nbDelete.getAndIncrement();
            }
        }

        private final class AddRateStatsThread extends StatsThread {
            private final String[] extraColumn = new String[1];

            private AddRateStatsThread() {
                super("Add%");
            }

            @Override
            void resetStats() {
                super.resetStats();
                nbAdd.set(0);
                nbDelete.set(0);
            }

            @Override
            String[] getAdditionalColumns() {
                final int nbAddStat = nbAdd.getAndSet(0);
                final int nbDelStat = nbDelete.getAndSet(0);
                final int total = nbAddStat + nbDelStat;

                extraColumn[0] = String.format("%.2f", total > 0 ? ((double) nbAddStat / total) * 100 : 0.0);

                return extraColumn;
            }
        }

        private final class AddWorkerThread extends WorkerThread {

            AddWorkerThread(Connection connection, ConnectionFactory connectionFactory) {
                super(connection, connectionFactory);
            }

            @Override
            public Promise<?, LdapException> performOperation(Connection connection, DataSource[] dataSources,
                    long currentTime) {
                if (needsDelete(currentTime)) {
                    DeleteRequest dr = Requests.newDeleteRequest(getDNEntryToRemove());
                    ResultHandler<Result> deleteHandler = new DeleteStatsHandler(currentTime);

                    return connection.deleteAsync(dr).onSuccess(deleteHandler).onFailure(deleteHandler);
                } else {
                    return performAddOperation(connection, currentTime);
                }
            }

            private Promise<Result, LdapException> performAddOperation(Connection connection, long currentTime) {
                try {
                    Entry entry;
                    synchronized (generator) {
                        entry = generator.readEntry();
                    }

                    AddRequest ar = Requests.newAddRequest(entry);
                    ResultHandler<Result> addHandler = new AddStatsHandler(currentTime, entry.getName().toString());
                    return connection.addAsync(ar).onSuccess(addHandler).onFailure(addHandler);
                } catch (IOException e) {
                    // faking an error result by notifying the Handler
                    UpdateStatsResultHandler<Result> resHandler = new UpdateStatsResultHandler<Result>(currentTime);
                    resHandler.handleError(LdapException.newErrorResult(ResultCode.OTHER, e));
                    return null;
                }
            }

            private boolean needsDelete(final long currentTime) {
                if (dnEntriesAdded.isEmpty() || delStrategy == DeleteStrategy.OFF) {
                    return false;
                }

                switch (delThreshold) {
                case SIZE_THRESHOLD:
                    return dnEntriesAdded.size() > sizeThreshold;
                case AGE_THRESHOLD:
                    long olderEntryTimestamp = dnEntriesAdded.firstKey();
                    return (olderEntryTimestamp + timeToWait) < currentTime;
                default:
                    return false;
                }
            }

            private String getDNEntryToRemove() {
                String removedEntry = null;

                while (removedEntry == null) {
                    long minKey = dnEntriesAdded.firstKey();
                    long maxKey = dnEntriesAdded.lastKey();
                    long randomIndex = Math.round(Math.random() * (maxKey - minKey) + minKey);
                    Long key = dnEntriesAdded.ceilingKey(randomIndex);

                    if (key != null) {
                        removedEntry = dnEntriesAdded.remove(key);
                    }
                }

                return removedEntry;
            }

        }

        private final ConcurrentSkipListMap<Long, String> dnEntriesAdded =
            new ConcurrentSkipListMap<Long, String>();
        private final ThreadLocal<Random> randomSeq = new ThreadLocal<Random>() {
            @Override
            protected Random initialValue() {
                return new Random();
            }
        };

        private EntryGenerator generator;
        private DeleteStrategy delStrategy;
        private DeleteThreshold delThreshold;
        private Integer sizeThreshold;
        private long timeToWait;
        private final AtomicInteger nbAdd = new AtomicInteger();
        private final AtomicInteger nbDelete = new AtomicInteger();

        private AddPerformanceRunner(final PerformanceRunnerOptions options) throws ArgumentException {
            super(options);
        }

        @Override
        WorkerThread newWorkerThread(Connection connection, ConnectionFactory connectionFactory) {
            return new AddWorkerThread(connection, connectionFactory);
        }

        @Override
        StatsThread newStatsThread() {
            return new AddRateStatsThread();
        }

        public void validate(MultiChoiceArgument<DeleteStrategy> delModeArg, IntegerArgument delSizeThresholdArg,
                                IntegerArgument delAgeThresholdArg) throws ArgumentException {
            super.validate();
            delStrategy = delModeArg.getTypedValue();
            // Check for inconsistent use cases
            if (delSizeThresholdArg.isPresent() && delAgeThresholdArg.isPresent()) {
                throw new ArgumentException(ERR_ADDRATE_THRESHOLD_SIZE_AND_AGE.get());
            } else if (delStrategy == DeleteStrategy.OFF
                && (delSizeThresholdArg.isPresent() || delAgeThresholdArg.isPresent())) {
                throw new ArgumentException(ERR_ADDRATE_DELMODE_OFF_THRESHOLD_ON.get());
            } else if (delStrategy == DeleteStrategy.RANDOM && delAgeThresholdArg.isPresent()) {
                throw new ArgumentException(ERR_ADDRATE_DELMODE_RAND_THRESHOLD_AGE.get());
            }

            if (delStrategy != DeleteStrategy.OFF) {
                delThreshold =
                    delAgeThresholdArg.isPresent() ? DeleteThreshold.AGE_THRESHOLD : DeleteThreshold.SIZE_THRESHOLD;
                if (delThreshold == DeleteThreshold.SIZE_THRESHOLD) {
                    sizeThreshold = delSizeThresholdArg.getIntValue();
                } else {
                    timeToWait = delAgeThresholdArg.getIntValue() * 1000000000L;
                }
            }
        }
    }

    private enum DeleteStrategy {
        OFF, RANDOM, FIFO;
    }

    private enum DeleteThreshold {
        SIZE_THRESHOLD, AGE_THRESHOLD, OFF;
    }

    private static final int EXIT_CODE_SUCCESS = 0;

    /**
     * The main method for AddRate tool.
     *
     * @param args
     *            The command-line arguments provided to this program.
     */
    public static void main(final String[] args) {
        final int retCode = new AddRate().run(args);
        System.exit(filterExitCode(retCode));
    }

    private BooleanArgument verbose;

    private BooleanArgument scriptFriendly;

    private AddRate() {
        // Nothing to do
    }

    AddRate(PrintStream out, PrintStream err) {
        super(out, err);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInteractive() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isScriptFriendly() {
        return scriptFriendly.isPresent();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isVerbose() {
        return verbose.isPresent();
    }

    int run(final String[] args) {
        // Create the command-line argument parser for use with this program.
        final LocalizableMessage toolDescription = INFO_ADDRATE_TOOL_DESCRIPTION.get();
        final ArgumentParser argParser =
            new ArgumentParser(AddRate.class.getName(), toolDescription, false, true, 1, 1, "template-file-path");

        ConnectionFactoryProvider connectionFactoryProvider;
        ConnectionFactory connectionFactory;
        AddPerformanceRunner runner;

        /* Entries generation parameters */
        IntegerArgument randomSeedArg;
        StringArgument resourcePathArg;
        StringArgument constantsArg;

        /* addrate specifics arguments */
        MultiChoiceArgument<DeleteStrategy> deleteMode;
        IntegerArgument deleteSizeThreshold;
        IntegerArgument deleteAgeThreshold;

        try {
            Utils.setDefaultPerfToolProperties();
            PerformanceRunnerOptions options = new PerformanceRunnerOptions(argParser, this);
            options.setSupportsGeneratorArgument(false);

            connectionFactoryProvider = new ConnectionFactoryProvider(argParser, this);
            runner = new AddPerformanceRunner(options);

            addCommonArguments(argParser);

            /* Entries generation parameters */
            resourcePathArg =
                new StringArgument("resourcepath", 'r', MakeLDIF.OPTION_LONG_RESOURCE_PATH, false, false, true,
                    INFO_PATH_PLACEHOLDER.get(), null, null, INFO_ADDRATE_DESCRIPTION_RESOURCE_PATH.get());
            argParser.addArgument(resourcePathArg);

            randomSeedArg =
                new IntegerArgument("randomseed", 'R', OPTION_LONG_RANDOM_SEED, false, false, true,
                    INFO_SEED_PLACEHOLDER.get(), 0, null, INFO_ADDRATE_DESCRIPTION_SEED.get());
            argParser.addArgument(randomSeedArg);

            constantsArg =
                new StringArgument("constant", 'g', MakeLDIF.OPTION_LONG_CONSTANT, false, true, true,
                    INFO_CONSTANT_PLACEHOLDER.get(), null, null, INFO_ADDRATE_DESCRIPTION_CONSTANT.get());
            argParser.addArgument(constantsArg);

            /* addrate specifics arguments */
            deleteMode =
                new MultiChoiceArgument<DeleteStrategy>("deletemode", 'C', "deleteMode", false, true,
                    INFO_DELETEMODE_PLACEHOLDER.get(), Arrays.asList(DeleteStrategy.values()), false,
                    INFO_ADDRATE_DESCRIPTION_DELETEMODE.get());
            deleteMode.setDefaultValue(DeleteStrategy.FIFO.toString());
            argParser.addArgument(deleteMode);

            deleteSizeThreshold =
                new IntegerArgument("deletesizethreshold", 's', "deleteSizeThreshold", false, true,
                    INFO_DELETESIZETHRESHOLD_PLACEHOLDER.get(), INFO_ADDRATE_DESCRIPTION_DELETESIZETHRESHOLD.get());
            deleteSizeThreshold.setDefaultValue(String.valueOf(10000));
            argParser.addArgument(deleteSizeThreshold);

            deleteAgeThreshold =
                new IntegerArgument("deleteagethreshold", 'a', "deleteAgeThreshold", false, true,
                    INFO_DELETEAGETHRESHOLD_PLACEHOLDER.get(), INFO_ADDRATE_DESCRIPTION_DELETEAGETHRESHOLD.get());
            argParser.addArgument(deleteAgeThreshold);
        } catch (final ArgumentException ae) {
            errPrintln(ERR_CANNOT_INITIALIZE_ARGS.get(ae.getMessage()));
            return ResultCode.CLIENT_SIDE_PARAM_ERROR.intValue();
        }

        // Parse the command-line arguments provided to this program.
        try {
            argParser.parseArguments(args);

            if (argParser.usageOrVersionDisplayed()) {
                return EXIT_CODE_SUCCESS;
            }

            connectionFactory = connectionFactoryProvider.getAuthenticatedConnectionFactory();
            runner.validate(deleteMode, deleteSizeThreshold, deleteAgeThreshold);
        } catch (final ArgumentException ae) {
            final LocalizableMessage message = ERR_ERROR_PARSING_ARGS.get(ae.getMessage());
            errPrintln(message);
            errPrintln(argParser.getUsageMessage());
            return ResultCode.CLIENT_SIDE_PARAM_ERROR.intValue();
        }

        final String templatePath = argParser.getTrailingArguments().get(0);

        runner.generator =
            MakeLDIF.createGenerator(templatePath, resourcePathArg, randomSeedArg, constantsArg, false, this);

        return runner.run(connectionFactory);
    }

    private void addCommonArguments(ArgumentParser argParser) throws ArgumentException {
        StringArgument propertiesFileArgument = CommonArguments.getPropertiesFile();
        argParser.addArgument(propertiesFileArgument);
        argParser.setFilePropertiesArgument(propertiesFileArgument);

        BooleanArgument noPropertiesFileArgument = CommonArguments.getNoPropertiesFile();
        argParser.addArgument(noPropertiesFileArgument);
        argParser.setNoPropertiesFileArgument(noPropertiesFileArgument);

        BooleanArgument showUsage = CommonArguments.getShowUsage();
        argParser.addArgument(showUsage);
        argParser.setUsageArgument(showUsage, getOutputStream());

        verbose = CommonArguments.getVerbose();
        argParser.addArgument(verbose);

        scriptFriendly =
            new BooleanArgument("scriptFriendly", 'S', "scriptFriendly", INFO_DESCRIPTION_SCRIPT_FRIENDLY.get());
        scriptFriendly.setPropertyName("scriptFriendly");
        argParser.addArgument(scriptFriendly);

    }
}
