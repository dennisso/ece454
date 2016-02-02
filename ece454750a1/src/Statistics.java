import ece454750s15a1.PerfCounters;

public class Statistics extends Object {
    private Integer requestsReceived = 0;
    private Integer requestsCompleted = 0;
    private long startTimeMillis = 0;

    public Statistics() {
        startTimeMillis = System.currentTimeMillis();
    }

    public Integer getRequestsReceived() {
        return requestsReceived;
    }

    public Integer getRequestsCompleted() {
        return requestsCompleted;
    }

    public void incrementRequestsReceived() {
        this.requestsReceived += 1;
    }

    public void incrementRequestsCompleted() {
        this.requestsCompleted += 1;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public Integer getAgeSeconds() {
        return (int) ((System.currentTimeMillis() - getStartTimeMillis()) / 1000);
    }

    public PerfCounters getPerfCounters() {
        return new PerfCounters(getAgeSeconds(), getRequestsReceived(), getRequestsCompleted());
    }
    @Override
    public String toString() {
        return "Statistics{" +
                "requestsReceived=" + requestsReceived +
                ", requestsCompleted=" + requestsCompleted +
                ", startTimeMillis=" + startTimeMillis +
                ", age=" + getAgeSeconds() +
                '}';
    }
}
