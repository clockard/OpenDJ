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
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 *      Portions Copyright 2013-2015 ForgeRock AS.
 */
package org.opends.server;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.testng.ForgeRockTestCase;
import org.opends.server.replication.ReplicationTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * This class defines a base test case that should be subclassed by all
 * unit tests used by the Directory Server.
 * <p>
 * This class adds the ability to print error messages and automatically
 * have them include the class name.
 */
@Test(sequential=true)
@SuppressWarnings("javadoc")
public abstract class DirectoryServerTestCase extends ForgeRockTestCase {

  @BeforeSuite
  public final void suppressOutput() {
    System.out.println("Replication DB implementation used in tests: '" +
        ReplicationTestCase.replicationDbImplementation + "'.");
    System.out.flush();

    TestCaseUtils.suppressOutput();
  }

  @AfterSuite
  public final void shutdownServer() {
    TestCaseUtils.shutdownServer(LocalizableMessage.raw("The current test suite has finished."));
    TestCaseUtils.unsupressOutput();
  }

  //
  // This is all a HACK to reduce the amount of memory that's consumed.
  //
  // This could be a problem if a subclass references a @DataProvider in
  // a super-class that provides static parameters, i.e. the parameters are
  // not regenerated for each invocation of the DataProvider.
  //

  /** A list of all parameters that were generated by a @DataProvider
   *  and passed to a test method of this class.  TestListener helps us
   *  keep this so that once all of the tests are finished, we can clear
   *  it out in an @AfterClass method.  We can't just clear it out right
   *  away in the TestListener because some methods share a @DataProvider.*/
  private final IdentityHashMap<Object[],Object> successfulTestParams = new IdentityHashMap<>();

  /** These are test parameters from a test that has failed.  We need to
   *  keep these around because the test report expects to find them when
   *  printing out failures. */
  private final IdentityHashMap<Object[],Object> failedTestParams = new IdentityHashMap<>();

  /**
   * Adds testParams to the list of all test parameters, so it can be
   * null'ed out later if it's not part.
   */
  void addParamsFromSuccessfulTests(Object[] testParams) {
    if (testParams != null) {
      successfulTestParams.put(testParams, testParams);
    }
  }

  /**
   * Adds testParams to the list of all failed test parameters, so that we
   * know to NOT null it out later.
   */
  void addParamsFromFailedTest(Object[] testParams) {
    if (testParams != null) {
      failedTestParams.put(testParams, testParams);
    }
  }

  /**
   * Null out all test parameters except the ones used in failed tests
   * since we might need these again.
   */
  @AfterClass(alwaysRun = true)
  public void clearSuccessfulTestParams2() {
    Set<Object[]> paramsSet = successfulTestParams.keySet();
    if (paramsSet == null) {  // Can this ever happen?
      return;
    }
    for (Object[] params: paramsSet) {
      if (failedTestParams.containsKey(params)) {
        continue;
      }

      for (int i = 0; i < params.length; i++) {
        params[i] = null;
      }
    }
    successfulTestParams.clear();
    failedTestParams.clear();
  }

  /**
   * The member variables of a test class can prevent lots of memory from being
   * reclaimed, so we use reflection to null out all of the member variables
   * after the tests have run.  Since all tests must inherit from
   * DirectoryServerTestCase, TestNG guarantees that this method runs after
   * all of the subclass methods, so this isn't too dangerous.
   */
  @AfterClass(alwaysRun = true)
  public void nullMemberVariablesAfterTest2() {
    Class<?> cls = this.getClass();
    // Iterate through all of the fields in all subclasses of
    // DirectoryServerTestCase, but not DirectoryServerTestCase itself.
    while (DirectoryServerTestCase.class.isAssignableFrom(cls) &&
           !DirectoryServerTestCase.class.equals(cls))
    {
      Field fields[] = cls.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        int modifiers = field.getModifiers();
        Class<?> fieldClass = field.getType();

        // If it's a non-static non-final non-primitive type, then null it out
        // so that the garbage collector can reclaim it and everything it
        // references.
        if (!fieldClass.isPrimitive() &&
            !fieldClass.isEnum()      &&
            !Modifier.isFinal(modifiers) &&
            !Modifier.isStatic(modifiers))
        {
          field.setAccessible(true);
          try {
            field.set(this, null);
          } catch (IllegalAccessException e) {
            // We're only doing this to save memory, so it's no big deal
            // if we can't set it.
          }
        }
      }
      cls = cls.getSuperclass();
    }
  }

  /** Accessed by listener on test class completion. */
  long startTime;
  long endTime;
  List<String> threadNamesBeforeClass;
  List<String> threadNamesAfterClass;

  @BeforeClass(alwaysRun = true)
  public void captureEnvironmentStateBeforeClass()
  {
    startTime = System.currentTimeMillis();
    threadNamesBeforeClass = listAllThreadNames();
  }

  @AfterClass(alwaysRun = true)
  public void captureEnvironmentStateAfterClass()
  {
    endTime = System.currentTimeMillis();
    threadNamesAfterClass = listAllThreadNames();
  }

  private List<String> listAllThreadNames()
  {
    Thread currentThread = Thread.currentThread();
    ThreadGroup topGroup = currentThread.getThreadGroup();
    while (topGroup.getParent() != null)
    {
      topGroup = topGroup.getParent();
    }

    Thread threads[] = new Thread[topGroup.activeCount() * 2];
    int numThreads = topGroup.enumerate(threads);

    List<String> activeThreads = new ArrayList<>();
    for (int i = 0; i < numThreads; i++)
    {
      Thread thread = threads[i];
      if (thread.isAlive())
      {
        String fullName = thread.getName();
        activeThreads.add(fullName);
      }
    }

    Collections.sort(activeThreads);
    return activeThreads;
  }
}
