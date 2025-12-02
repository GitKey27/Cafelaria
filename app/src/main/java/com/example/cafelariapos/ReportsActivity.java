package com.example.cafelariapos;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsActivity extends AppCompatActivity {
    private LinearLayout container;
    private SharedPreferences prefs;
    private Gson gson;
    private static final String PREFS_NAME = "UserData";
    private static final String KEY_SALES = "sales";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        container = findViewById(R.id.reportsContainer);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        gson = new Gson();

        loadAndRenderReports();
    }

    private void loadAndRenderReports() {
        String json = prefs.getString(KEY_SALES, null);
        List<Sale> sales = new ArrayList<>();
        if (json != null) {
            sales = gson.fromJson(json, new TypeToken<List<Sale>>(){}.getType());
            if (sales == null) sales = new ArrayList<>();
        }

        double revenue = 0.0;
        Map<String, Integer> productCounts = new HashMap<>();
        for (Sale s : sales) {
            revenue += s.getTotal();
            for (SaleItem it : s.getItems()) {
                productCounts.put(it.getName(), productCounts.getOrDefault(it.getName(), 0) + it.getQuantity());
            }
        }

        TextView summary = new TextView(this);
        summary.setText("Sales: " + sales.size() + "   Revenue: ₱" + String.format(Locale.getDefault(), "%.2f", revenue));
        summary.setTextSize(16);
        summary.setTextColor(Color.BLACK);
        container.addView(summary);

        TextView top = new TextView(this);
        top.setText("\nTop Products:\n");
        top.setTextSize(14);
        top.setTextColor(Color.BLACK);
        container.addView(top);

        for (Map.Entry<String, Integer> e : productCounts.entrySet()) {
            TextView t = new TextView(this);
            t.setText(e.getKey() + " x " + e.getValue());
            t.setTextColor(Color.BLACK);
            container.addView(t);
        }

        TextView recentLabel = new TextView(this);
        recentLabel.setText("\nRecent Sales:\n");
        recentLabel.setTextSize(14);
        recentLabel.setTextColor(Color.BLACK);
        container.addView(recentLabel);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        for (int i = Math.max(0, sales.size() - 10); i < sales.size(); i++) {
            Sale s = sales.get(i);
            TextView saleTv = new TextView(this);
            saleTv.setText(df.format(new Date(s.getTimestamp())) + "  -  ₱" + String.format(Locale.getDefault(), "%.2f", s.getTotal()));
            saleTv.setTextColor(Color.BLACK);
            container.addView(saleTv);
        }

        if (sales.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("No sales yet.");
            none.setTextColor(Color.BLACK);
            container.addView(none);
        }
    }
}
