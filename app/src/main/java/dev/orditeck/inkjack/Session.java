package dev.orditeck.inkjack;

/** Content deliberately lives only in this process. No disk history or screenshot cache. */
final class Session {
    static final TextTools.Collection collection = new TextTools.Collection();
    static String result = "";
    static boolean importRunning;
    static boolean importReady;
    static String importError;
    private Session() {}
}
