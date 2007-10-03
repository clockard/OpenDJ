package org.opends.guitools.statuspanel.browser.ui;

import org.opends.quicksetup.ui.UIFactory;

import javax.swing.*;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.WeakHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.awt.*;

/**
 */
public class StatusPanel extends JPanel implements Status {

  private static Logger LOG = Logger.getLogger(StatusPanel.class.getName());

  private static final long serialVersionUID = -2110906650411270393L;

  private static final int STATUS_REAPER_FREQUENCY_MS = 1000;

  private static final int STATUS_DISPLAYER_FREQUENCY_MS = 1000;

  private JLabel lblStatus = UIFactory.makeJLabel(null, null, UIFactory.TextStyle.SECONDARY_FIELD_VALID);

  private StatusMessage currentMessage = null;

  private PriorityQueue<StatusMessage> messageQueue =
          new PriorityQueue<StatusMessage>();

  private Map<Long, StatusMessage> messageMap = new WeakHashMap<Long, StatusMessage>();

  public StatusPanel() {
    setLayout(new BorderLayout());
    add(lblStatus, BorderLayout.WEST);
    new Thread(new MessageDisplayer(messageQueue),
              "Status Message Displayer").start();
    new Thread(new MessageReaper(), "Status Message Reaper").start();
  }

  synchronized public Long wait(final String s) {
    Long id = null;
    if (s != null) {
      StatusMessage msg = new StatusMessage(s, StatusMessage.Type.WAIT, 5);
      id = msg.getId();
      messageQueue.add(msg);
      messageMap.put(id, msg);
    }
    return id;
  }

  synchronized public void done(Long messageId) {
    StatusMessage msg = messageMap.remove(messageId);
    if (msg != null) {
      messageQueue.remove(msg);
    }
  }

  synchronized public void error(String text) {
    if (text != null) {
      StatusMessage msg = new StatusMessage(text, StatusMessage.Type.ERROR, 1);
      messageQueue.add(msg);
    }
  }

  synchronized public void warning(String text) {
    if (text != null) {
      StatusMessage msg = new StatusMessage(text, StatusMessage.Type.WARNING, 2);
      messageQueue.add(msg);
    }
  }

  private void updateLabel(final StatusMessage message) {
    if (SwingUtilities.isEventDispatchThread()) {
      updateLabel2(message);
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          updateLabel2(message);
        }
      });
    }
  }

  synchronized private void updateLabel2(final StatusMessage message) {
    currentMessage = message;
    if (message != null) {
      message.shown(System.currentTimeMillis());
      lblStatus.setText(message.toString());
      switch (message.getType()) {
        case ERROR:
          lblStatus.setIcon(UIFactory.getImageIcon(UIFactory.IconType.ERROR));
          break;
        case WARNING:
          lblStatus.setIcon(UIFactory.getImageIcon(UIFactory.IconType.WARNING));
          break;
        case WAIT:
          lblStatus.setIcon(UIFactory.getImageIcon(UIFactory.IconType.WAIT_TINY));
          break;
      }
    } else {
      lblStatus.setText("");
      lblStatus.setIcon(null);
    }
  }

  private class MessageDisplayer implements Runnable {

    PriorityQueue<StatusMessage> messages;

    MessageDisplayer(PriorityQueue<StatusMessage> messages) {
      this.messages = messages;
    }

    public void run() {
      while (true) {
        try {
          if (currentMessage != null &&
                  currentMessage.isExpired()) {
            updateLabel(null);
          }

          StatusMessage nextMessage = messages.poll();
          if (nextMessage != null) {
            System.out.println("nextm=" + nextMessage.toString());
            System.out.println("nextstale=" + nextMessage.isStale());
            if (currentMessage != null) {
              System.out.println("currentexp=" + currentMessage.isExpired());
              System.out.println("currentprio=" + currentMessage.getPriority());
              System.out.println("nextprio=" + nextMessage.getPriority());
            }
            while (true) {
              System.out.println("nextm=" + nextMessage.toString());
              System.out.println("nextstale=" + nextMessage.isStale());
              if (currentMessage != null) {
                System.out.println("currentexp=" + currentMessage.isExpired());
                System.out.println("currentprio=" + currentMessage.getPriority());
                System.out.println("nextprio=" + nextMessage.getPriority());
              }
              if (!nextMessage.isStale()) {
                if (currentMessage == null ||
                            currentMessage.isExpired() &&
                            currentMessage.getPriority() >= nextMessage.getPriority()) {
                  break;
                }
              }
              nextMessage = messages.poll();
              if (nextMessage == null) break;
            }
          }

          if (nextMessage != null) {
            System.out.println("Showing " + nextMessage);
            updateLabel(nextMessage);
          }

          Thread.sleep(STATUS_DISPLAYER_FREQUENCY_MS);
        } catch (InterruptedException e) {
          LOG.log(Level.INFO, "message canceler interrupted", e);
        }
      }
    }

  }

  private class MessageReaper implements Runnable {

    public void run() {
      while (true) {
        try {
          if (currentMessage != null && currentMessage.isExpired()) {
            updateLabel(null);
          }
          Thread.sleep(STATUS_REAPER_FREQUENCY_MS);
        } catch (InterruptedException e) {
          LOG.log(Level.INFO, "message canceler interrupted", e);
        }
      }
    }

  }

  static private class StatusMessage implements Comparable<StatusMessage> {

    public enum Type { INFO, WARNING, ERROR, WAIT }

    String msg;
    Type type;
    Long created;
    Integer priority;
    Long shown;

    /** Number of seconds this message should remain in the status area */
    Long duration;

    StatusMessage(String msg, Type type, int priority) {
      this.msg = msg;
      this.type = type;
      this.created = System.currentTimeMillis();
      this.priority = priority;
      switch(type) {
        case INFO: duration = 1000L; break;
        case WARNING: duration = 10000L; break;
        case ERROR: duration = 10000L; break;
        case WAIT: duration = 1000L; break;
      }
    }

    public Type getType() {
      return this.type;
    }

    public void shown(Long timestamp) {
      this.shown = timestamp;
    }

    public int getPriority() {
      return priority;
    }

    public boolean isExpired() {
      return (shown != null && System.currentTimeMillis() - shown > duration);
    }

    public boolean isStale() {
      return (created != null && System.currentTimeMillis() - created > duration);
    }

    public long getId() {
      return this.created;
    }

    public long getDelay(TimeUnit unit) {
      return unit.convert(duration, TimeUnit.MILLISECONDS);
    }

    public int compareTo(Delayed o) {
      return new Long(this.getDelay(TimeUnit.MILLISECONDS)).
              compareTo(o.getDelay(TimeUnit.MILLISECONDS));
    }

    public int compareTo(StatusMessage o) {
      return priority.compareTo(o.priority);
    }

    public String toString() {
      return msg;
    }
  }

}
