package eu.mikroskeem.picomaven;

/**
 * @author Mark Vainomaa
 */
public interface DebugLoggerImpl {
    void debug(String format, Object... contents);

    class DummyDebugLogger implements DebugLoggerImpl {
        public final static DummyDebugLogger INSTANCE = new DummyDebugLogger();
        @Override public void debug(String format, Object... contents) {}
    }
}
