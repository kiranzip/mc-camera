package zip.kiran.snapshot.client;

public final class ClientCameraState {
    private boolean viewfinderOn;
    private boolean wasActive;
    private long lastShutterMillis;

    public boolean isViewfinderOn() {
        return viewfinderOn;
    }

    public void setViewfinderOn(boolean value) {
        viewfinderOn = value;
    }

    public boolean wasActive() {
        return wasActive;
    }

    public void setWasActive(boolean value) {
        wasActive = value;
    }

    public long getLastShutterMillis() {
        return lastShutterMillis;
    }

    public void setLastShutterMillis(long value) {
        lastShutterMillis = value;
    }

    public void reset() {
        viewfinderOn = false;
        wasActive = false;
        lastShutterMillis = 0L;
    }
}