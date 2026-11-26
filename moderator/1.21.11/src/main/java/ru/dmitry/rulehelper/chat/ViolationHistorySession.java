package ru.dmitry.rulehelper.chat;

import java.util.ArrayList;
import java.util.List;

/** Collects server chat output for one /hist request. */
public final class ViolationHistorySession {
   public enum State { WAITING, COMPLETE, TIMEOUT, CANCELLED }

   private static final long QUIET_PERIOD_MS = 2_500L;
   private static final long MAX_WAIT_MS = 45_000L;
   private static ViolationHistorySession active;

   private final String playerName;
   private final long startedAt;
   private final ViolationHistoryCounter counter = new ViolationHistoryCounter();
   private final List<String> messages = new ArrayList<>();
   private long lastMessageAt;
   private int serverMessages;
   private State state = State.WAITING;

   private ViolationHistorySession(String playerName) {
      this.playerName = playerName;
      this.startedAt = System.currentTimeMillis();
      this.lastMessageAt = this.startedAt;
   }

   public static ViolationHistorySession begin(String playerName) {
      active = new ViolationHistorySession(playerName);
      return active;
   }

   /** Stops any pending hidden capture before a normal history command is sent to chat. */
   public static void cancelActive() {
      ViolationHistorySession session = active;
      if (session != null) session.cancel();
   }

   /** Captures a history response and tells the chat mixin to suppress that message locally. */
   public static boolean captureAndSuppressServerMessage(String message) {
      ViolationHistorySession session = active;
      if (session == null || session.state != State.WAITING) return false;
      long now = System.currentTimeMillis();
      if (now - session.startedAt > MAX_WAIT_MS) {
         session.state = State.TIMEOUT;
         session.counter.finish();
         return false;
      }
      session.serverMessages++;
      session.lastMessageAt = now;
      session.messages.add(message);
      session.counter.accept(message);
      return true;
   }

   public void update() {
      if (this.state != State.WAITING) return;
      long now = System.currentTimeMillis();
      if (now - this.startedAt >= MAX_WAIT_MS) {
         this.state = State.TIMEOUT;
         this.counter.finish();
      } else if (this.serverMessages > 0 && now - this.lastMessageAt >= QUIET_PERIOD_MS) {
         this.state = State.COMPLETE;
         this.counter.finish();
      }
   }

   public void cancel() {
      if (this.state == State.WAITING) this.state = State.CANCELLED;
      if (active == this) active = null;
   }

   public String playerName() { return this.playerName; }
   public State state() { return this.state; }
   public int serverMessages() { return this.serverMessages; }
   public ViolationHistoryCounter counter() { return this.counter; }
   public List<String> messages() { return List.copyOf(this.messages); }
}
