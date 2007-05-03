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
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */
package org.opends.server.loggers.debug;

import static org.opends.server.util.ServerConstants.EOL;

/**
 * A DebugStackTraceFormatter converts an exception's stack trace into
 * a String appropriate for tracing, optionally performing filtering
 * of stack frames.
 */
public class DebugStackTraceFormatter
{
  /**
   * The stack depth value to indicate the entire stack should be printed.
   */
  public static final int COMPLETE_STACK= Integer.MAX_VALUE;
  /**
  * A nested frame filter that removes debug and trailing no OpenDS frames.
  */
  public static final FrameFilter SMART_FRAME_FILTER = new SmartFrameFilter();

  /**
   * A FrameFilter provides stack frame filtering used during formatting.
   */
  public interface FrameFilter {

    /**
     * Filters out all undesired stack frames from the given Throwable's
     * stack trace.
     * @param frames the frames to filter
     * @return an array of StackTraceElements to be used in formatting.
     */
    public StackTraceElement[] getFilteredStackTrace(
        StackTraceElement[] frames);
  }

  /**
   * A basic FrameFilter that filters out frames from the debug logging and
   * non OpenDS classes.
   */
  private static class SmartFrameFilter implements FrameFilter {

    private boolean isFrameForPackage(StackTraceElement frame,
                                      String packageName)
    {
      boolean isContained= false;

      if (frame != null) {
        String className= frame.getClassName();
        isContained= className != null && className.startsWith(packageName);
      }
      return isContained;
    }

    /**
     * Return the stack trace of an exception with debug and trailing non
     * OpenDS frames filtered out.
     *
     * @param frames the frames to filter
     * @return the filtered stack trace.
     */
    public StackTraceElement[] getFilteredStackTrace(
        StackTraceElement[] frames)
    {
      StackTraceElement[] trimmedStack= null;
      if (frames != null && frames.length > 0) {
        int firstFrame= 0;

        // Skip leading frames debug logging classes
        while (firstFrame < frames.length &&
            isFrameForPackage(frames[firstFrame],
                              "org.opends.server.loggers")) {
          firstFrame++;
        }

        // Skip trailing frames not in OpenDS classes
        int lastFrame= frames.length - 1;
        while (lastFrame > firstFrame &&
            !isFrameForPackage(frames[lastFrame], "org.opends")) {
          lastFrame--;
        }

        trimmedStack= new StackTraceElement[lastFrame - firstFrame + 1];
        for (int i= firstFrame; i <= lastFrame; i++) {
          trimmedStack[i - firstFrame]= frames[i];
        }
      }

      return trimmedStack;
    }
  }

  /**
   * Generate a String representation of the entire stack trace of the
   * given Throwable.
   * @param t - the Throwable for which to generate the stack trace.
   * @return the stack trace.
   */
  public static String formatStackTrace(Throwable t)
  {
    return formatStackTrace(t, null, COMPLETE_STACK, true);
  }

  /**
   * Generate a String representation of the possibly filtered stack trace
   * of the given Throwable.
   * @param t - the Throwable for which to generate the stack trace.
   * @param filter - a FrameFilter to use to exclude some stack frames from
   * the trace.  If null, no filtering is performed.
   * @param maxDepth - the maximum number of stack frames to include in the
   * trace.
   * @param includeCause - also include the stack trace for the cause Throwable.
   * @return the stack trace.
   */
  public static String formatStackTrace(Throwable t, FrameFilter filter,
                                        int maxDepth, boolean includeCause)
  {
    StringBuffer buffer= new StringBuffer();

    while(t != null)
    {
      StackTraceElement[] frames = t.getStackTrace();
      if(filter != null)
      {
        frames = filter.getFilteredStackTrace(frames);
      }

      if (frames != null) {
        int frameLimit=  Math.min(maxDepth, frames.length);
        if (frameLimit > 0) {


          for (int i= 0; i < frameLimit; i++) {
            buffer.append("  ");
            buffer.append(frames[i]);
            buffer.append(EOL);
          }

          if(frameLimit < frames.length)
          {
            buffer.append("  ...(");
            buffer.append(frames.length - frameLimit);
            buffer.append(" more)");
            buffer.append(EOL);
          }
        }
      }

      if(includeCause && t.getCause() != null)
      {
        t = t.getCause();
        buffer.append("  caused by ");
      }
      else
      {
        t = null;
      }
    }

    return buffer.toString();
  }

  /**
   * Generate a String representation of the possibly filtered stack trace
   * from the current position in executation.
   * @param filter - a FrameFilter to use to exclude some stack frames from
   * the trace.  If null, no filtering is performed.
   * @param maxDepth - the maximum number of stack frames to include in the
   * trace.
   * @return the stack trace.
   */
  public static String formatStackTrace(FrameFilter filter, int maxDepth)
  {
    StringBuffer buffer= new StringBuffer();

    StackTraceElement[] frames = Thread.currentThread().getStackTrace();
    if(filter != null)
    {
      frames = filter.getFilteredStackTrace(frames);
    }

    if (frames != null) {
      int frameLimit=  Math.min(maxDepth, frames.length);
      if (frameLimit > 0) {


        for (int i= 0; i < frameLimit; i++) {
          buffer.append("  ");
          buffer.append(frames[i]);
          buffer.append(EOL);
        }

        if(frameLimit < frames.length)
        {
          buffer.append("  ...(");
          buffer.append(frames.length - frameLimit);
          buffer.append(" more)");
          buffer.append(EOL);
        }
      }
    }

    return buffer.toString();
  }
}
