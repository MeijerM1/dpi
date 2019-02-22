package shared.messaging;

import shared.model.bank.BankInterestReply;

import java.util.*;

/**
 * @author Max Meijer
 * Created on 22/02/2019
 */
public class BankReplyAggregator {
    private int expectedReplies;
    private List<BankInterestReply> replies = new ArrayList<>();
    private int aggregation;
    private String correlation;

    public BankReplyAggregator(int expectedReplies, int aggregation, String correlation) {
        this.expectedReplies = expectedReplies;
        this.aggregation = aggregation;
        this.correlation = correlation;
    }

    public void addReply(BankInterestReply reply) {
        replies.add(reply);
    }

    public boolean isReplyComplete() {
        return  expectedReplies == replies.size();
    }

    public BankInterestReply getBestOffer() {
        return Collections.min(replies, Comparator.comparing(BankInterestReply::getInterest));
    }

    public int getExpectedReplies() {
        return expectedReplies;
    }

    public void setExpectedReplies(int expectedReplies) {
        this.expectedReplies = expectedReplies;
    }

    public int getAggregation() {
        return aggregation;
    }

    public void setAggregation(int aggregation) {
        this.aggregation = aggregation;
    }

    public String getCorrelation() {
        return correlation;
    }

    public void setCorrelation(String correlation) {
        this.correlation = correlation;
    }
}
