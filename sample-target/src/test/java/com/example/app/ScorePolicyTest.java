package com.example.app;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ScorePolicyTest {

    @Test
    public void isPassed_whenScoreIs60_shouldReturnTrue() throws Exception {
        ScorePolicy target = new ScorePolicy();

        boolean result = target.isPassed(60);

        assertTrue(result);
    }

    @Test
    public void isPassed_whenScoreIs59_shouldReturnFalse() throws Exception {
        ScorePolicy target = new ScorePolicy();

        boolean result = target.isPassed(59);

        assertFalse(result);
    }

    @Test
    public void levelName_withDefaultInputs_shouldRun() throws Exception {
        ScorePolicy target = new ScorePolicy();

        String result = target.levelName(1);

        assertNotNull(result);
    }

    @Test
    public void levelName_whenScoreIsZero_shouldRun() throws Exception {
        ScorePolicy target = new ScorePolicy();

        String result = target.levelName(0);

        assertNotNull(result);
    }

    @Test
    public void levelName_whenScoreIsNegative_shouldRun() throws Exception {
        ScorePolicy target = new ScorePolicy();

        String result = target.levelName(-1);

        assertNotNull(result);
    }

}
