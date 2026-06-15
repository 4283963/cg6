package com.underpass.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DepthFilterService {

    private final int windowSize;
    private final double spikeRejectRatio;
    private final Map<String, Deque<Double>> slidingWindows = new ConcurrentHashMap<>();
    private final Map<String, Double> lastStableDepth = new ConcurrentHashMap<>();

    public DepthFilterService() {
        this.windowSize = 5;
        this.spikeRejectRatio = 2.0;
    }

    public DepthFilterService(int windowSize, double spikeRejectRatio) {
        this.windowSize = windowSize;
        this.spikeRejectRatio = spikeRejectRatio;
    }

    public double filter(String underpassId, double rawDepthMm) {
        Deque<Double> window = slidingWindows.computeIfAbsent(underpassId, k -> new LinkedList<>());
        Double lastStable = lastStableDepth.get(underpassId);

        double validated = rawDepthMm;
        if (lastStable != null && lastStable > 0) {
            double ratio = rawDepthMm / lastStable;
            if (ratio > spikeRejectRatio || ratio < (1.0 / spikeRejectRatio)) {
                log.warn("Spike detected for {}: raw={}mm, lastStable={}mm, ratio={:.2f} — rejected, using lastStable",
                        underpassId, rawDepthMm, lastStable, ratio);
                validated = lastStable;
            }
        }

        window.addLast(validated);
        while (window.size() > windowSize) {
            window.removeFirst();
        }

        double smoothed = window.stream().mapToDouble(d -> d).average().orElse(validated);
        lastStableDepth.put(underpassId, smoothed);

        if (Math.abs(rawDepthMm - smoothed) > 20) {
            log.info("Depth filtered for {}: raw={}mm -> smoothed={}mm (window={})",
                    underpassId, rawDepthMm, smoothed, window.size());
        }

        return smoothed;
    }

    public Double getSmoothedDepth(String underpassId) {
        return lastStableDepth.get(underpassId);
    }

    public void reset(String underpassId) {
        slidingWindows.remove(underpassId);
        lastStableDepth.remove(underpassId);
    }
}
