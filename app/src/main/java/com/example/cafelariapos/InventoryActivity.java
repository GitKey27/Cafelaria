package com.example.cafelariapos;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class InventoryActivity extends AppCompatActivity {
    private LinearLayout container;
    private SharedPreferences prefs;
    private Gson gson;
    private Map<String, Product> productMap = new TreeMap<>();
    // holds pending (unconfirmed) signed deltas per product id: positive = adds, negative = subtracts
    private Map<String, Integer> pendingDeltas = new HashMap<>();
    // footer buttons rendered by renderInventory()
    private static final String PREFS_NAME = "UserData";
    private static final String KEY_PRODUCTS = "products";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        gson = new Gson();

        container = findViewById(R.id.inventoryContainer);

        loadProducts();
        renderInventory();

        findViewById(R.id.btnAddProduct).setOnClickListener(v -> showAddProductDialog());

        findViewById(R.id.btnClearAll).setOnClickListener(v -> {
            new AlertDialog.Builder(InventoryActivity.this)
                .setTitle("Clear Inventory")
                .setMessage("Are you sure you want to delete all products?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    productMap.clear();
                    saveProducts();
                    renderInventory();
                    Toast.makeText(InventoryActivity.this, "Inventory Cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
        });
    }

    private void loadProducts() {
        String json = prefs.getString(KEY_PRODUCTS, null);
        productMap.clear();
        if (json != null) {
            List<Product> list = gson.fromJson(json, new TypeToken<List<Product>>(){}.getType());
            if (list != null) {
                for (Product p : list) productMap.put(p.getId(), p);
            }
        }

        if (productMap.isEmpty()) {
            seedDefaultProducts();
            saveProducts();
        }
    }

    private void saveProducts() {
        List<Product> list = new ArrayList<>(productMap.values());
        prefs.edit().putString(KEY_PRODUCTS, gson.toJson(list)).apply();
    }

    private void seedDefaultProducts() {
        productMap.clear();
        addSample(new Product("p_coffee_beans", "Coffee Beans", 12.99, 50));
        addSample(new Product("p_espresso_machine", "Espresso Machine", 299.99, 10));
        addSample(new Product("p_milk_frother", "Milk Frother", 25.00, 30));
    }

    private void addSample(Product p) {
        productMap.put(p.getId(), p);
    }

    private void renderInventory() {
        container.removeAllViews();
        for (Product p : productMap.values()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 8, 0, 8);
            row.setLayoutParams(lp);

            TextView name = new TextView(this);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            name.setText(p.getName());
            name.setTextColor(Color.BLACK);

            final TextView stock = new TextView(this);
            stock.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f));
            // show current stock plus any pending (unconfirmed) delta (positive or negative)
            int pending = pendingDeltas.containsKey(p.getId()) ? pendingDeltas.get(p.getId()) : 0;
            String pendingSuffix = "";
            if (pending > 0) pendingSuffix = " ( +" + pending + ")";
            else if (pending < 0) pendingSuffix = " ( " + pending + ")"; // negative already has '-'
            stock.setText(String.format(Locale.getDefault(), "%d%s", p.getStock() + pending, pendingSuffix));
            stock.setTextColor(Color.BLACK);

            Button btnInc = new Button(this);
            btnInc.setText("+");

            // When '+' is tapped, accumulate a pending addition for this product (no save yet)
            btnInc.setOnClickListener(v -> {
                int cur = pendingDeltas.containsKey(p.getId()) ? pendingDeltas.get(p.getId()) : 0;
                pendingDeltas.put(p.getId(), cur + 1);
                int pendingNow = pendingDeltas.get(p.getId());
                String suffix = pendingNow > 0 ? " ( +" + pendingNow + ")" : (pendingNow < 0 ? " ( " + pendingNow + ")" : "");
                stock.setText(String.format(Locale.getDefault(), "%d%s", p.getStock() + pendingNow, suffix));
                renderFooterButtonsIfNeeded();
            });

            Button btnDec = new Button(this);
            btnDec.setText("-");
            btnDec.setOnClickListener(v -> {
                int curPending = pendingDeltas.containsKey(p.getId()) ? pendingDeltas.get(p.getId()) : 0;
                // Prevent pending decrements that would make resulting stock negative
                if (p.getStock() + curPending <= 0) {
                    Toast.makeText(this, "Cannot reduce below zero", Toast.LENGTH_SHORT).show();
                    return;
                }
                pendingDeltas.put(p.getId(), curPending - 1);
                int pendingNow = pendingDeltas.get(p.getId());
                String suffix = pendingNow > 0 ? " ( +" + pendingNow + ")" : (pendingNow < 0 ? " ( " + pendingNow + ")" : "");
                stock.setText(String.format(Locale.getDefault(), "%d%s", p.getStock() + pendingNow, suffix));
                renderFooterButtonsIfNeeded();
            });

            row.addView(name);
            row.addView(stock);
            row.addView(btnInc);
            row.addView(btnDec);
            container.addView(row);
        }

        // After listing all products, render Confirm/Cancel footer if there are pending adds
        renderFooterButtonsIfNeeded();
    }

    // Adds a footer with Confirm All and Cancel buttons when pendingAdds is non-empty.
    private void renderFooterButtonsIfNeeded() {
        // remove any existing footer if present (we clear container at start of renderInventory, but this
        // helper may be called multiple times, so ensure we don't duplicate footers)
        // First, remove any previous footer views with tag "pendingFooter"
        for (int i = container.getChildCount() - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            Object tag = child.getTag();
            if (tag != null && "pendingFooter".equals(tag)) {
                container.removeViewAt(i);
            }
        }

        // only show footer if there is at least one non-zero pending delta
        boolean hasPending = false;
        for (Integer v : pendingDeltas.values()) if (v != 0) { hasPending = true; break; }
        if (!hasPending) return;

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setTag("pendingFooter");
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        flp.setMargins(0, 12, 0, 12);
        footer.setLayoutParams(flp);

        Button btnConfirmAll = new Button(this);
        btnConfirmAll.setText("Confirm All");
        btnConfirmAll.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnConfirmAll.setOnClickListener(v -> {
            // apply all pending signed deltas; clamp to zero to avoid negative stock
            for (Map.Entry<String, Integer> e : pendingDeltas.entrySet()) {
                Product prod = productMap.get(e.getKey());
                if (prod == null) continue;
                int result = prod.getStock() + e.getValue();
                if (result < 0) result = 0;
                prod.setStock(result);
            }
            pendingDeltas.clear();
            saveProducts();
            renderInventory();
            Toast.makeText(InventoryActivity.this, "Stock updates confirmed", Toast.LENGTH_SHORT).show();
        });

        Button btnCancelAll = new Button(this);
        btnCancelAll.setText("Cancel");
        btnCancelAll.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnCancelAll.setOnClickListener(v -> {
            pendingDeltas.clear();
            renderInventory();
            Toast.makeText(InventoryActivity.this, "Pending updates cancelled", Toast.LENGTH_SHORT).show();
        });

        footer.addView(btnConfirmAll);
        footer.addView(btnCancelAll);
        container.addView(footer);
    }

    private void showAddProductDialog() {
        final EditText etId = findViewById(R.id.etNewProductId);
        final EditText etName = findViewById(R.id.etNewProductName);
        final EditText etPrice = findViewById(R.id.etNewProductPrice);
        final EditText etStock = findViewById(R.id.etNewProductStock);

        String id = etId.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String priceS = etPrice.getText().toString().trim();
        String stockS = etStock.getText().toString().trim();

        if (id.isEmpty() || name.isEmpty() || priceS.isEmpty() || stockS.isEmpty()) {
            Toast.makeText(this, "Please fill product fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceS);
            stock = Integer.parseInt(stockS);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price/stock", Toast.LENGTH_SHORT).show();
            return;
        }

        if (productMap.containsKey(id)) {
            Toast.makeText(this, "Product id exists", Toast.LENGTH_SHORT).show();
            return;
        }

        Product p = new Product(id, name, price, stock);
        productMap.put(id, p);
        saveProducts();
        renderInventory();

        etId.setText("");
        etName.setText("");
        etPrice.setText("");
        etStock.setText("");
        Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show();
    }
}
