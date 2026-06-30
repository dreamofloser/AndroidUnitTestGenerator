package com.example.app;

public class ScorePolicy {
    public boolean isPassed(int score) {
        return score >= 60;
    }

    public String levelName(int score) {
        if (score >= 90) {
            return "A";
        }

        if (score >= 60) {
            return "B";
        }

        return "C";
    }
}
