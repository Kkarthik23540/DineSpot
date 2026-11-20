package com.example.dinespot;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AdminAnalyticsActivity extends CommonBaseActivity {

    private TextView tvUsersVal, tvResVal, tvBookVal, tvFeedVal;
    private TextView tvBestSeller1, tvBestSeller2, tvPeakTime, tvPeakDay;
    private FirebaseFirestore db;
    private LineChart lineChart;
    private Button btnFilterRange;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View analyticsScroll, analyticsSkeleton;
    private View filterToday, filterWeekly, filterMonthly;
    private int currentRangeDays = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_analytics);
        setLightStatusBar(false); // Teal background needs light icons

        setupToolbar("System Analytics", true);

        tvUsersVal = findViewById(R.id.tvUsersVal);
        tvResVal = findViewById(R.id.tvResVal);
        tvBookVal = findViewById(R.id.tvBookVal);
        tvFeedVal = findViewById(R.id.tvFeedVal);
        
        tvBestSeller1 = findViewById(R.id.tvBestSeller1);
        tvBestSeller2 = findViewById(R.id.tvBestSeller2);
        tvPeakTime = findViewById(R.id.tvPeakTime);
        tvPeakDay = findViewById(R.id.tvPeakDay);

        lineChart = findViewById(R.id.lineChart);
        btnFilterRange = findViewById(R.id.btnFilterRange);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        analyticsScroll = findViewById(R.id.analytics_scroll);
        analyticsSkeleton = findViewById(R.id.analytics_skeleton);

        filterToday = findViewById(R.id.filter_today);
        filterWeekly = findViewById(R.id.filter_weekly);
        filterMonthly = findViewById(R.id.filter_monthly);

        setupCharts();
        setupFilters();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                fetchAnalyticsData();
                fetchChartData();
            });
        }

        btnFilterRange.setOnClickListener(v -> showRangePopup());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        CommonNavigationHelper.setupAdminNavigation(this, bottomNavigationView, R.id.admin_analytics);

        db = FirebaseFirestore.getInstance();
        fetchAnalyticsData();
        fetchChartData();
    }

    private void setupFilters() {
        View.OnClickListener clickListener = v -> {
            resetFilterUI();
            MaterialCardView card = (MaterialCardView) v;
            TextView tv = (TextView) card.getChildAt(0);

            card.setStrokeColor(Color.WHITE);
            card.setCardBackgroundColor(Color.WHITE);
            tv.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));

            int id = v.getId();
            if (id == R.id.filter_today) currentRangeDays = 1;
            else if (id == R.id.filter_weekly) currentRangeDays = 7;
            else if (id == R.id.filter_monthly) currentRangeDays = 30;

            fetchChartData();
        };

        filterToday.setOnClickListener(clickListener);
        filterWeekly.setOnClickListener(clickListener);
        filterMonthly.setOnClickListener(clickListener);
    }

    private void resetFilterUI() {
        int[] ids = {R.id.filter_today, R.id.filter_weekly, R.id.filter_monthly};
        for (int id : ids) {
            MaterialCardView card = findViewById(id);
            if (card != null) {
                TextView tv = (TextView) card.getChildAt(0);
                card.setStrokeColor(Color.parseColor("#4DFFFFFF"));
                card.setCardBackgroundColor(Color.TRANSPARENT);
                tv.setTextColor(Color.WHITE);
            }
        }
    }

    private void showRangePopup() {
        PopupMenu popup = new PopupMenu(this, btnFilterRange);
        popup.getMenu().add(0, 7, 0, "Last 7 Days");
        popup.getMenu().add(0, 30, 1, "Last 30 Days");
        popup.getMenu().add(0, 90, 2, "Last 3 Months");
        popup.getMenu().add(0, 365, 3, "Last Year");

        popup.setOnMenuItemClickListener(item -> {
            currentRangeDays = item.getItemId();
            btnFilterRange.setText(item.getTitle());
            fetchChartData();
            return true;
        });
        popup.show();
    }

    private void setupCharts() {
        // Line Chart
        lineChart.setDrawGridBackground(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setExtraOffsets(5f, 5f, 5f, 15f);

        XAxis lxAxis = lineChart.getXAxis();
        lxAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        lxAxis.setDrawGridLines(false);
        lxAxis.setGranularity(1f);
        lxAxis.setLabelRotationAngle(-45f);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getLegend().setEnabled(false);
    }

    private void fetchChartData() {
        db.collection("bookings").get().addOnSuccessListener(snapshots -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (snapshots == null) return;
            
            hideSkeleton();

            Map<String, Integer> dailyCounts = new TreeMap<>(); 
            Map<String, Integer> itemFrequency = new HashMap<>();
            Map<String, Integer> hourFrequency = new HashMap<>();
            Map<String, Integer> dayFrequency = new HashMap<>();

            SimpleDateFormat keySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat hourSdf = new SimpleDateFormat("hh a", Locale.getDefault());
            SimpleDateFormat daySdf = new SimpleDateFormat("EEEE", Locale.getDefault());

            for (int j = 0; j < currentRangeDays; j++) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DAY_OF_YEAR, -j);
                dailyCounts.put(keySdf.format(c.getTime()), 0);
            }

            for (QueryDocumentSnapshot doc : snapshots) {
                String dateTimeStr = doc.getString("dateTime");
                
                // Process Items for Best Sellers
                List<Map<String, Object>> items = (List<Map<String, Object>>) doc.get("orderedItems");
                if (items != null) {
                    for (Map<String, Object> item : items) {
                        String itemName = (String) item.get("name");
                        if (itemName != null) {
                            itemFrequency.put(itemName, itemFrequency.getOrDefault(itemName, 0) + 1);
                        }
                    }
                }

                if (dateTimeStr != null) {
                    try {
                        Date date = fullSdf.parse(dateTimeStr);
                        if (date != null) {
                            String dateKey = keySdf.format(date);
                            Integer dailyCount = dailyCounts.get(dateKey);
                            if (dailyCount != null) {
                                dailyCounts.put(dateKey, dailyCount + 1);
                            }
                            
                            // Process Time/Day for Insights
                            String hourKey = hourSdf.format(date);
                            String dayKey = daySdf.format(date);
                            hourFrequency.put(hourKey, hourFrequency.getOrDefault(hourKey, 0) + 1);
                            dayFrequency.put(dayKey, dayFrequency.getOrDefault(dayKey, 0) + 1);
                        }
                    } catch (Exception ignored) {}
                }
            }
            updateLineChart(dailyCounts);
            updatePopularityInsights(itemFrequency, hourFrequency, dayFrequency);
        }).addOnFailureListener(e -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            hideSkeleton();
        });
    }

    private void updatePopularityInsights(Map<String, Integer> itemFreq, Map<String, Integer> hourFreq, Map<String, Integer> dayFreq) {
        // Best Sellers
        List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(itemFreq.entrySet());
        sortedItems.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        
        if (sortedItems.size() > 0) tvBestSeller1.setText("1. " + sortedItems.get(0).getKey());
        else tvBestSeller1.setText("No data yet");
        
        if (sortedItems.size() > 1) tvBestSeller2.setText("2. " + sortedItems.get(1).getKey());
        else tvBestSeller2.setText("");

        // Peak Hour
        String peakH = getPeakKey(hourFreq);
        tvPeakTime.setText(peakH != null ? peakH : "N/A");

        // Peak Day
        String peakD = getPeakKey(dayFreq);
        tvPeakDay.setText(peakD != null ? "Busiest on " + peakD : "");
    }

    private String getPeakKey(Map<String, Integer> freq) {
        String peak = null;
        int max = -1;
        for (Map.Entry<String, Integer> entry : freq.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                peak = entry.getKey();
            }
        }
        return peak;
    }

    private void updateLineChart(Map<String, Integer> dailyCounts) {
        List<Entry> entries = new ArrayList<>();
        List<String> rawKeys = new ArrayList<>(dailyCounts.keySet());
        final List<String> displayLabels = new ArrayList<>();
        
        SimpleDateFormat keySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displaySdf = new SimpleDateFormat("MMM dd", Locale.getDefault());

        for (int k = 0; k < rawKeys.size(); k++) {
            String key = rawKeys.get(k);
            Integer count = dailyCounts.get(key);
            entries.add(new Entry(k, count != null ? count : 0));
            try {
                Date parsedDate = keySdf.parse(key);
                if (parsedDate != null) {
                    displayLabels.add(displaySdf.format(parsedDate));
                } else {
                    displayLabels.add(key);
                }
            } catch (Exception e) {
                displayLabels.add(key);
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Bookings");
        dataSet.setColor(Color.parseColor("#118D9D"));
        dataSet.setCircleColor(Color.parseColor("#118D9D"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#118D9D"));
        dataSet.setFillAlpha(40);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                return (index >= 0 && index < displayLabels.size()) ? displayLabels.get(index) : "";
            }
        });
        lineChart.invalidate();
    }

    private void fetchAnalyticsData() {
        db.collection("users").get().addOnSuccessListener(snaps -> CommonAnimationHelper.animateStatChange(tvUsersVal, snaps.size()));
        db.collection("restaurants").get().addOnSuccessListener(snaps -> CommonAnimationHelper.animateStatChange(tvResVal, snaps.size()));
        db.collection("bookings").get().addOnSuccessListener(snaps -> CommonAnimationHelper.animateStatChange(tvBookVal, snaps.size()));
        db.collection("feedback").get().addOnSuccessListener(snaps -> CommonAnimationHelper.animateStatChange(tvFeedVal, snaps.size()));
    }

    private void hideSkeleton() {
        if (analyticsSkeleton != null) analyticsSkeleton.setVisibility(View.GONE);
        if (analyticsScroll != null) analyticsScroll.setVisibility(View.VISIBLE);
    }
}
