/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2007 Sun Microsystems, Inc.
 */

package org.opends.server.authorization.dseecompat;

import static org.opends.server.messages.AciMessages.*;
import static org.opends.server.authorization.dseecompat.Aci.*;
import static org.opends.server.messages.MessageHandler.getMessage;
import org.opends.server.types.AttributeType;
import org.opends.server.types.DN;
import org.opends.server.types.SearchScope;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents target part of an ACI's syntax. This is the part
 * of an ACI before the ACI body and specifies the entry, attributes, or set
 * of entries and attributes which the ACI controls access.
 *
 * The five supported  ACI target keywords are: target, targetattr,
 * targetscope, targetfilter and targattrfilters.
 */
public class AciTargets {

    /*
     * ACI syntax has a target keyword.
     */
    private Target target = null ;

    /*
     * ACI syntax has a targetscope keyword.
     */
    private SearchScope targetScope = SearchScope.WHOLE_SUBTREE;

    /*
     * ACI syntax has a targetattr keyword.
     */
    private TargetAttr targetAttr = null ;

    /*
     * ACI syntax has a targetfilter keyword.
     */
    private TargetFilter targetFilter=null;

    /*
     * ACI syntax has a targattrtfilters keyword.
     */
    private TargAttrFilters targAttrFilters=null;

    /*
     * The number of regular expression group positions in a valid ACI target
     * expression.
     */
    private static final int targetElementCount = 3;

    /*
     *  Regular expression group position of a target keyword.
     */
    private static final int targetKeywordPos       = 1;

    /*
     *  Regular expression group position of a target operator enumeration.
     */
    private static final int targetOperatorPos      = 2;

    /*
     *  Regular expression group position of a target expression statement.
     */
    private static final int targetExpressionPos    = 3;

    /*
     * Regular expression used to match a single target rule.
     */
    private static final String targetRegex =
           OPEN_PAREN +  ZERO_OR_MORE_WHITESPACE  +  WORD_GROUP +
           ZERO_OR_MORE_WHITESPACE + "(!?=)" + ZERO_OR_MORE_WHITESPACE +
           "\"([^\"]+)\"" + ZERO_OR_MORE_WHITESPACE + CLOSED_PAREN +
           ZERO_OR_MORE_WHITESPACE;

    /**
    * Regular expression used to match one or more target rules. The patern is
    * part of a general ACI verification.
    */
    public static final String targetsRegex = "(" + targetRegex + ")*";

    /*
     * Rights that are skipped for certain target evaluations.
     * The test is use the skipRights array is:
     *
     * Either the ACI has a targetattr's rule and the current
     * attribute type is null or the current attribute type has
     * a type specified and the targetattr's rule is null.
     *
     * The actual check against the skipRights array is:
     *
     *  1. Is the ACI's rights in this array? For example,
     *     allow(all) or deny(add)
     *
     *  AND
     *
     *  2. Is the rights from the LDAP operation in this array? For
     *      example, an LDAP add would have rights of add and all.
     *
     *  If both are true, than the target match test returns true
     *  for this ACI.
     */

    private static final int skipRights = (ACI_ADD | ACI_DELETE | ACI_PROXY);

    /**
     * Creates an ACI target from the specified arguments. All of these
     * may be null -- the ACI has no targets an will use defaults.
     * @param targetEntry The ACI target keyword if any.
     * @param targetAttr The ACI targetattr keyword if any.
     * @param targetFilter The ACI targetfilter keyword if any.
     * @param targetScope The ACI targetscope keyword if any.
     * @param targAttrFilters The ACI targAttrFilters keyword if any.
     */
    private AciTargets(Target targetEntry, TargetAttr targetAttr,
                       TargetFilter targetFilter,
                       SearchScope targetScope,
                       TargAttrFilters targAttrFilters) {
       this.target=targetEntry;
       this.targetAttr=targetAttr;
       this.targetScope=targetScope;
       this.targetFilter=targetFilter;
       this.targAttrFilters=targAttrFilters;
    }

    /**
     * Return class representing the ACI target keyword. May be
     * null. The default is the use the DN of the entry containing
     * the ACI and check if the resource entry is a descendant of that.
     * @return The ACI target class.
     */
    public Target getTarget() {
        return target;
    }

    /**
     * Return class representing the ACI targetattr keyword. May be null.
     * The default is to not match any attribute types in an entry.
     * @return The ACI targetattr class.
     */
    public TargetAttr getTargetAttr() {
        return targetAttr;
    }

    /**
     * Return the ACI targetscope keyword. Default is WHOLE_SUBTREE.
     * @return The ACI targetscope information.
     */
    public SearchScope getTargetScope() {
        return targetScope;
    }

    /**
     * Return class representing the  ACI targetfilter keyword. May be null.
     * @return The targetscope information.
     */
    public TargetFilter getTargetFilter() {
        return targetFilter;
    }

    /**
     * Return the class representing the ACI targattrfilters keyword. May be
     * null.
     * @return The targattrfilters information.
     */
    public TargAttrFilters getTargAttrFilters() {
        return targAttrFilters;
    }
    /**
     * Decode an ACI's target part of the syntax from the string provided.
     * @param input String representing an ACI target part of syntax.
     * @param dn The DN of the entry containing the ACI.
     * @return An AciTargets class representing the decoded ACI target string.
     * @throws AciException If the provided string contains errors.
     */
    public static AciTargets decode(String input, DN dn)
    throws AciException {
        Target target=null;
        TargetAttr targetAttr=null;
        TargetFilter targetFilter=null;
        TargAttrFilters targAttrFilters=null;
        SearchScope targetScope=SearchScope.WHOLE_SUBTREE;
        Pattern targetPattern = Pattern.compile(targetRegex);
        Matcher targetMatcher = targetPattern.matcher(input);
        while (targetMatcher.find())
        {
            if (targetMatcher.groupCount() != targetElementCount) {
                int msgID = MSGID_ACI_SYNTAX_INVALID_TARGET_SYNTAX;
                String message = getMessage(msgID, input);
                throw new AciException(msgID, message);
            }
            String keyword = targetMatcher.group(targetKeywordPos);
            EnumTargetKeyword targetKeyword  =
                EnumTargetKeyword.createKeyword(keyword);
            if (targetKeyword == null) {
                int msgID = MSGID_ACI_SYNTAX_INVALID_TARGET_KEYWORD;
                String message = getMessage(msgID, keyword  );
                throw new AciException(msgID, message);
            }
            String operator =
                targetMatcher.group(targetOperatorPos);
            EnumTargetOperator targetOperator =
                EnumTargetOperator.createOperator(operator);
            if (targetOperator == null) {
                int msgID = MSGID_ACI_SYNTAX_INVALID_TARGETS_OPERATOR;
                String message = getMessage(msgID, operator);
                throw new AciException(msgID, message);
            }
            String expression = targetMatcher.group(targetExpressionPos);
            switch(targetKeyword)
            {
            case KEYWORD_TARGET:
            {
                if (target == null){
                    target =  Target.decode(targetOperator, expression, dn);
                }
                else
                {
                    int msgID =
                        MSGID_ACI_SYNTAX_INVALID_TARGET_DUPLICATE_KEYWORDS;
                    String message =
                        getMessage(msgID, "target", input);
                    throw new AciException(msgID, message);
                }
                break;
            }
            case KEYWORD_TARGETATTR:
            {
                if (targetAttr == null){
                    targetAttr = TargetAttr.decode(targetOperator,
                            expression);
                }
                else {
                    int msgID =
                        MSGID_ACI_SYNTAX_INVALID_TARGET_DUPLICATE_KEYWORDS;
                    String message =
                        getMessage(msgID, "targetattr", input);
                    throw new AciException(msgID, message);
                }
                break;
            }
            case KEYWORD_TARGETSCOPE:
            {
                // Check the operator for the targetscope is EQUALITY
                if (targetOperator == EnumTargetOperator.NOT_EQUALITY) {
                    int msgID = MSGID_ACI_SYNTAX_INVALID_TARGET_NOT_OPERATOR;
                    String message = getMessage(msgID, operator);
                    throw new AciException(msgID, message);
                }
                targetScope=createScope(expression);
                break;
            }
            case KEYWORD_TARGETFILTER:
            {
                if (targetFilter == null){
                    targetFilter = TargetFilter.decode(targetOperator,
                            expression);
                }
                else {
                    int msgID =
                            MSGID_ACI_SYNTAX_INVALID_TARGET_DUPLICATE_KEYWORDS;
                    String message =
                            getMessage(msgID, "targetfilter", input);
                    throw new AciException(msgID, message);
                }
                break;
            }
            case KEYWORD_TARGATTRFILTERS:
            {
                if (targAttrFilters == null){
                    // Check the operator for the targattrfilters is EQUALITY
                    if (targetOperator == EnumTargetOperator.NOT_EQUALITY) {
                        int msgID =
                                MSGID_ACI_SYNTAX_INVALID_TARGET_NOT_OPERATOR;
                        String message = getMessage(msgID, operator);
                        throw new AciException(msgID, message);
                    }
                    targAttrFilters = TargAttrFilters.decode(targetOperator,
                            expression);
                }
                else {
                    int msgID =
                            MSGID_ACI_SYNTAX_INVALID_TARGET_DUPLICATE_KEYWORDS;
                    String message =
                            getMessage(msgID, "targattrfilters", input);
                    throw new AciException(msgID, message);
                }
                break;
            }
            }
        }
        return new AciTargets(target, targetAttr, targetFilter,
                              targetScope, targAttrFilters);
    }

    /**
     * Evaluates a provided scope string and returns an appropriate
     * SearchScope enumeration.
     * @param expression The expression string.
     * @return An search scope enumeration matching the string.
     * @throws AciException If the expression is an invalid targetscope
     * string.
     */
    private static SearchScope createScope(String expression)
    throws AciException {
        if(expression.equalsIgnoreCase("base"))
                return SearchScope.BASE_OBJECT;
        else if(expression.equalsIgnoreCase("onelevel"))
            return SearchScope.SINGLE_LEVEL;
        else if(expression.equalsIgnoreCase("subtree"))
            return SearchScope.WHOLE_SUBTREE;
        else if(expression.equalsIgnoreCase("subordinate"))
            return SearchScope.SUBORDINATE_SUBTREE;
        else {
            int msgID =
                MSGID_ACI_SYNTAX_INVALID_TARGETSCOPE_EXPRESSION;
            String message = getMessage(msgID, expression);
            throw new AciException(msgID, message);
        }
    }

    /**
     * Checks an ACI's targetfilter information against a target match
     * context.
     * @param aci The ACI to try an match the targetfilter of.
     * @param matchCtx The target match context containing information needed
     * to perform a target match.
     * @return True if the targetfilter matched the target context.
     */
    public static boolean isTargetFilterApplicable(Aci aci,
                                              AciTargetMatchContext matchCtx) {
        boolean ret=true;
        TargetFilter targetFilter=aci.getTargets().getTargetFilter();
        if(targetFilter != null)
             ret=targetFilter.isApplicable(matchCtx);
        return ret;
    }

    /**
     * Check an ACI's targattrfilters against a target match context.
     * @param aci The ACI to match the targattrfilters against.
     * @param matchCtx  The target match context containing the information
     * needed to perform the target match.
     * @return True if the targattrfilters matched the target context.
     */
    public static boolean isTargAttrFiltersApplicable(Aci aci,
                                               AciTargetMatchContext matchCtx) {
        boolean ret=true;
        TargAttrFilters targAttrFilters=aci.getTargets().getTargAttrFilters();
        if(targAttrFilters != null) {
            if((matchCtx.hasRights(ACI_ADD) &&
                targAttrFilters.hasMask(TARGATTRFILTERS_ADD)) ||
              (matchCtx.hasRights(ACI_DELETE) &&
               targAttrFilters.hasMask(TARGATTRFILTERS_DELETE)))
                ret=targAttrFilters.isApplicableAddDel(matchCtx);
            else if((matchCtx.hasRights(ACI_WRITE_ADD) &&
                     targAttrFilters.hasMask(TARGATTRFILTERS_ADD)) ||
                    (matchCtx.hasRights(ACI_WRITE_DELETE) &&
                    targAttrFilters.hasMask(TARGATTRFILTERS_DELETE)))
                ret=targAttrFilters.isApplicableMod(matchCtx, aci);
        }
        return ret;
    }

    /*
     * TODO Evaluate making this method more efficient.
     * The isTargetAttrApplicable method looks a lot less efficient than it
     * could be with regard to the logic that it employs and the repeated use
     * of method calls over local variables.
     */
    /**
     * Checks an provided ACI's targetattr information against a target match
     * context.
     * @param aci The ACI to evaluate.
     * @param targetMatchCtx The target match context to check the ACI against.
     * @return True if the targetattr matched the target context.
     */
    public static boolean isTargetAttrApplicable(Aci aci,
                                         AciTargetMatchContext targetMatchCtx) {
        boolean ret=true;
        if(!targetMatchCtx.getTargAttrFiltersMatch()) {
            AciTargets targets=aci.getTargets();
            AttributeType a=targetMatchCtx.getCurrentAttributeType();
            int rights=targetMatchCtx.getRights();
            boolean isFirstAttr=targetMatchCtx.isFirstAttribute();
            if((a != null) && (targets.getTargetAttr() != null))  {
             ret=TargetAttr.isApplicable(a,targets.getTargetAttr());
             targetMatchCtx.clearACIEvalAttributesRule(ACI_ATTR_STAR_MATCHED);
             /*
               If a explicitly defined targetattr's match rule has not
               been seen (~ACI_FOUND_ATTR_RULE) and the current attribute type
               is applicable because of a targetattr all attributes rule match,
               set a flag to indicate this situation (ACI_ATTR_STAR_MATCHED).
               Else the attributes is applicable because it is operational or
               not a targetattr's all attribute match.
              */
             if(ret && targets.getTargetAttr().isAllAttributes() &&
                !targetMatchCtx.hasACIEvalAttributes())
               targetMatchCtx.setACIEvalAttributesRule(ACI_ATTR_STAR_MATCHED);
              else
                targetMatchCtx.setACIEvalAttributesRule(ACI_FOUND_ATTR_RULE);
            } else if((a != null) || (targets.getTargetAttr() != null)) {
                if((aci.hasRights(skipRights)) &&
                                                (skipRightsHasRights(rights)))
                    ret=true;
                else if ((targets.getTargetAttr() != null) &&
                        (a == null) && (aci.hasRights(ACI_WRITE)))
                    ret = true;
                else
                    ret = false;
            }
            if((isFirstAttr) && (aci.getTargets().getTargetAttr() == null)
                && aci.getTargets().getTargAttrFilters() == null)
                targetMatchCtx.setEntryTestRule(true);
        }
        return ret;
    }

    /**
     * Try and match a one or more of the specified rights in the skiprights
     * mask.
     * @param rights The rights to check for.
     * @return  True if the one or more of the specified rights are in the
     * skiprights rights mask.
     */
    public static boolean skipRightsHasRights(int rights) {
      //geteffectiverights sets this flag, turn it off before evaluating.
      int tmpRights=rights & ~ACI_SKIP_PROXY_CHECK;
      return  ((skipRights & tmpRights) == tmpRights);
    }


    /**
     * Wrapper class that passes an ACI, an ACI's targets and the specified
     * target match context's resource entry DN to the main isTargetApplicable
     * method.
     * @param aci The ACI currently be matched.
     * @param matchCtx The target match context to match against.
     * @return True if the target matched the ACI.
     */
    public static boolean isTargetApplicable(Aci aci,
                                             AciTargetMatchContext matchCtx) {
        return isTargetApplicable(aci, aci.getTargets(),
                                        matchCtx.getResourceEntry().getDN());
    }

    /*
     * TODO Investigate supporting alternative representations of the scope.
     *
     * Should we also consider supporting alternate representations of the
     * scope values (in particular, allow "one" in addition to "onelevel"
     * and "sub" in addition to "subtree") to match the very common
     * abbreviations in widespread use for those terms?
     */
    /**
     * Main target isApplicable method. This method performs the target keyword
     * match functionality, which allows for directory entry "targeting" using
     * the specifed ACI, ACI targets class and DN.
     * @param aci The ACI to match the target against.
     * @param targets The targets to use in this evaluation.
     * @param entryDN The DN to use in this evaluation.
     * @return True if the ACI matched the target and DN.
     */

    public static boolean isTargetApplicable(Aci aci,
            AciTargets targets, DN entryDN) {
        boolean ret=true;
        DN targetDN=aci.getDN();
        /*
         * Scoping of the ACI uses either the DN of the entry
         * containing the ACI (aci.getDN above), or if the ACI item
         * contains a simple target DN and a equality operator, that
         * simple target DN is used as the target DN.
         */
        if((targets.getTarget() != null) &&
                (!targets.getTarget().isPattern())) {
            EnumTargetOperator op=targets.getTarget().getOperator();
            if(op != EnumTargetOperator.NOT_EQUALITY)
                targetDN=targets.getTarget().getDN();
        }
        //Check if the scope is correct.
        switch(targets.getTargetScope()) {
        case BASE_OBJECT:
            if(!targetDN.equals(entryDN))
                return false;
            break;
        case SINGLE_LEVEL:
            /**
             * We use the standard definition of single level to mean the
             * immediate children only -- not the target entry itself.
             * Sun CR 6535035 has been raised on DSEE:
             * Non-standard interpretation of onelevel in ACI targetScope.
             */
            if(!entryDN.getParent().equals(targetDN))
                return false;
            break;
        case WHOLE_SUBTREE:
            if(!entryDN.isDescendantOf(targetDN))
                return false;
            break;
        case SUBORDINATE_SUBTREE:
            if ((entryDN.getNumComponents() <= targetDN.getNumComponents()) ||
                 !entryDN.isDescendantOf(targetDN)) {
              return false;
            }
            break;
        default:
            return false;
        }
        /*
         * The entry is in scope. For inequality checks, scope was tested
         * against the entry containing the ACI. If operator is inequality,
         * check that it doesn't match the target DN.
         */
        if((targets.getTarget() != null) &&
                (!targets.getTarget().isPattern())) {
            EnumTargetOperator op=targets.getTarget().getOperator();
            if(op == EnumTargetOperator.NOT_EQUALITY) {
                DN tmpDN=targets.getTarget().getDN();
                if(entryDN.isDescendantOf(tmpDN))
                    return false;
            }
        }
        /*
         * There is a pattern, need to match the substring filter
         * created when the ACI was decoded. If inequality flip the
         * result.
         */
        if((targets.getTarget() != null) &&
                (targets.getTarget().isPattern()))  {
            ret=targets.getTarget().matchesPattern(entryDN);
            EnumTargetOperator op=targets.getTarget().getOperator();
            if(op == EnumTargetOperator.NOT_EQUALITY)
                ret=!ret;
        }
        return ret;
    }
}
