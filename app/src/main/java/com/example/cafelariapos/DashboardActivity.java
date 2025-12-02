package com.example.cafelariapos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvWelcome;
    private LinearLayout tvOrderList; // now a container for order lines
    private TextView tvTotal;
    private Button btnLogout, btnCheckout, btnInventory, btnReports, btnRemoveProduct;
    private GridLayout productsContainer;

    private Button btnShowDrinks, btnShowPastries; // new section buttons
    private String currentSection = "Drinks"; // default to Drinks view

    // Discount state
    private Button btnDiscount;
    private int activeDiscountPercent = 0; // 0 = no discount
    private String activeDiscountCode = null;

    private SharedPreferences prefs;
    private Gson gson;

    private Map<String, Product> productMap = new HashMap<>();

    private Map<String, Integer> order = new HashMap<>();

    private double total = 0.0;

    private boolean removalMode = false; // retained but not used now

    private static final String PREFS_NAME = "UserData";
    private static final String KEY_PRODUCTS = "products";
    private static final String KEY_SALES = "sales";
    private static final int LOW_STOCK_THRESHOLD = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        gson = new Gson();

        tvWelcome = findViewById(R.id.tvWelcome);
        tvOrderList = findViewById(R.id.tvOrderList);
        tvTotal = findViewById(R.id.tvTotal);
        btnLogout = findViewById(R.id.btnLogout);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnInventory = findViewById(R.id.btnInventory);
        btnReports = findViewById(R.id.btnReports);
        productsContainer = findViewById(R.id.productsContainer);
        btnRemoveProduct = findViewById(R.id.btnRemoveProduct);
        TextView tvAlerts = findViewById(R.id.tvAlerts);

        // section buttons
        btnShowDrinks = findViewById(R.id.btnShowDrinks);
        btnShowPastries = findViewById(R.id.btnShowPastries);

        // discount button
        btnDiscount = findViewById(R.id.btnDiscount);

        // Hide the top Remove button because each order line now has its own remove icon
        if (btnRemoveProduct != null) {
            btnRemoveProduct.setVisibility(View.GONE);
        }

        loadProducts();
        // initialize tints for section buttons
        updateSectionButtonTints();
        renderProducts();
        updateLowStockAlerts(tvAlerts);

        String firstName = getIntent().getStringExtra("firstName");
        String username = getIntent().getStringExtra("username");
        if (firstName == null || firstName.isEmpty()) firstName = username;
        tvWelcome.setText("Welcome, " + (firstName == null ? "" : firstName) + "!");

        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        btnCheckout.setOnClickListener(v -> doCheckout());

        btnInventory.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, InventoryActivity.class)));
        btnReports.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ReportsActivity.class)));

        // section button listeners
        btnShowDrinks.setOnClickListener(v -> {
            currentSection = "Drinks";
            updateSectionButtonTints();
            renderProducts();
        });

        btnShowPastries.setOnClickListener(v -> {
            currentSection = "Pastries";
            updateSectionButtonTints();
            renderProducts();
        });

        // discount button listener
        if (btnDiscount != null) {
            btnDiscount.setOnClickListener(v -> openDiscountDialog());
        }

        rebuildOrderView();
    }

    private void updateSectionButtonTints() {
        if (btnShowDrinks == null || btnShowPastries == null) return;
        if ("Drinks".equals(currentSection)) {
            btnShowDrinks.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            btnShowPastries.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.secondary));
        } else if ("Pastries".equals(currentSection)) {
            btnShowDrinks.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.secondary));
            btnShowPastries.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
        } else {
            btnShowDrinks.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            btnShowPastries.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.secondary));
        }
    }

    private void updateLowStockAlerts(TextView tvAlerts) {
        StringBuilder sb = new StringBuilder();
        for (Product p : productMap.values()) {
            if (p.getStock() <= LOW_STOCK_THRESHOLD) {
                sb.append(p.getName()).append(" (stock: ").append(p.getStock()).append(")\n");
            }
        }
        if (sb.length() > 0) {
            tvAlerts.setText("Low stock:\n" + sb.toString());
            tvAlerts.setVisibility(View.VISIBLE);
        } else {
            tvAlerts.setVisibility(View.GONE);
        }
    }

    private void loadProducts() {
        String json = prefs.getString(KEY_PRODUCTS, null);
        if (json != null) {
            try {
                List<Product> list = gson.fromJson(json, new TypeToken<List<Product>>(){}.getType());
                if (list != null) {
                    productMap.clear();
                    for (Product p : list) productMap.put(p.getId(), p);
                }
            } catch (Exception e) {
                productMap.clear();
            }
        }

        if (productMap.isEmpty()) seedDefaultProducts();
    }

    private void saveProducts() {
        List<Product> list = new ArrayList<>(productMap.values());
        String json = gson.toJson(list);
        prefs.edit().putString(KEY_PRODUCTS, json).apply();
        TextView tvAlerts = findViewById(R.id.tvAlerts);
        updateLowStockAlerts(tvAlerts);
    }

    private void seedDefaultProducts() {
        productMap.clear();
        addProduct(new Product("p_espresso", "Espresso", 60.00, 20, "Hot Drinks", 5, "ESP-001", 0.12));
        addProduct(new Product("p_americano", "Americano", 75.00, 20, "Hot Drinks", 5, "AME-001", 0.12));
        addProduct(new Product("p_cappuccino", "Cappuccino", 95.00, 15, "Hot Drinks", 5, "CAP-001", 0.12));
        addProduct(new Product("p_latte", "Latte", 100.00, 15, "Hot Drinks", 5, "LAT-001", 0.12));
        addProduct(new Product("p_mocha", "Mocha", 110.00, 10, "Hot Drinks", 5, "MOC-001", 0.12));
        addProduct(new Product("p_coldbrew", "Cold Brew", 120.00, 12, "Cold Drinks", 5, "CBR-001", 0.12));
        addProduct(new Product("p_iced_latte", "Iced Latte", 105.00, 14, "Cold Drinks", 5, "ICL-001", 0.12));
        addProduct(new Product("p_matcha", "Matcha Latte", 115.00, 8, "Hot Drinks", 5, "MAT-001", 0.12));
        addProduct(new Product("p_caramel_mac", "Caramel Macchiato", 125.00, 10, "Hot Drinks", 5, "CRM-001", 0.12));
        addProduct(new Product("p_croissant", "Croissant", 55.00, 25, "Bakery", 5, "CRO-001", 0.12));
        addProduct(new Product("p_muffin", "Blueberry Muffin", 60.00, 18, "Bakery", 5, "MUF-001", 0.12));
        addProduct(new Product("p_bagel", "Plain Bagel", 40.00, 30, "Bakery", 5, "BAG-001", 0.12));
        addProduct(new Product("p_brownie", "Brownie", 55.00, 8, "Bakery", 5, "BRN-001", 0.12));
        saveProducts();

        String salesJson = prefs.getString(KEY_SALES, null);
        if (salesJson == null || salesJson.isEmpty()) {
            try {
                List<SaleItem> items = new ArrayList<>();
                items.add(new SaleItem("p_cappuccino", "Cappuccino", 2, 140.00));
                long ts;
                try {

                    ts = System.currentTimeMillis();
                } catch (Exception ex) {
                    ts = System.currentTimeMillis();
                }
                Sale sampleSale = new Sale("o_101", items, 313.6, ts);
                List<Sale> list = new ArrayList<>();
                list.add(sampleSale);
                prefs.edit().putString(KEY_SALES, gson.toJson(list)).apply();
            } catch (Throwable t) {
            }
        }

        final String KEY_SUMMARY = "sales_summary";
        if (prefs.getString(KEY_SUMMARY, null) == null) {
            SalesSummary summary = new SalesSummary("2025-10-23", 3450.00, 42, 82.14);
            prefs.edit().putString(KEY_SUMMARY, gson.toJson(summary)).apply();
        }
    }

    private void addProduct(Product p) {
        productMap.put(p.getId(), p);
    }

    private void renderProducts() {
        productsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int cardWidth = getResources().getDisplayMetrics().widthPixels / 2 - (int)(24 * getResources().getDisplayMetrics().density);
        for (Product p : productMap.values()) {
            // Apply section filter: show only drinks or pastries if selected
            String cat = p.getCategory() == null ? "" : p.getCategory().toLowerCase(Locale.ROOT);
            boolean isDrink = cat.contains("drink");
            boolean isBakery = cat.contains("bakery") || cat.contains("pastry");

            if ("Drinks".equals(currentSection) && !isDrink) continue;
            if ("Pastries".equals(currentSection) && !isBakery) continue;

            View card = inflater.inflate(R.layout.product_card, productsContainer, false);
            TextView tvName = card.findViewById(R.id.tvProductName);
            TextView tvPrice = card.findViewById(R.id.tvProductPrice);
            TextView tvOriginal = card.findViewById(R.id.tvProductOriginalPrice);
            Button btnAdd = card.findViewById(R.id.btnAddProduct);

            tvName.setText(p.getName());

            double price = p.getPrice();
            if (activeDiscountPercent > 0) {
                double discounted = price * (1 - (activeDiscountPercent / 100.0));
                // show original price (grayed and struck-through)
                tvOriginal.setText(String.format(Locale.getDefault(), "₱%.2f", price));
                tvOriginal.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                tvOriginal.setPaintFlags(tvOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvOriginal.setVisibility(View.VISIBLE);

                tvPrice.setText(String.format(Locale.getDefault(), "₱%.2f", discounted));
                tvPrice.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            } else {
                tvOriginal.setVisibility(View.GONE);
                tvPrice.setText(String.format(Locale.getDefault(), "₱%.2f", price));
                tvPrice.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }

            // Stock display is intentionally hidden in the layout for drinks; do not attempt to show it here.

            btnAdd.setOnClickListener(v -> {
                onProductClicked(p.getId());
            });

            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            int marginPx = (int) (8 * getResources().getDisplayMetrics().density);
            glp.setMargins(marginPx, marginPx, marginPx, marginPx);
            card.setLayoutParams(glp);

            productsContainer.addView(card);
        }
    }

    private void onProductClicked(String productId) {
        Product p = productMap.get(productId);
        if (p == null) return;

        String cat = p.getCategory() == null ? "" : p.getCategory().toLowerCase(Locale.ROOT);
        boolean isDrink = cat.contains("drink");

        if (!isDrink) {
            // keep stock checks for non-drinks (e.g., pastries/bakery)
            if (p.getStock() <= 0) {
                Toast.makeText(this, "Out of stock: " + p.getName(), Toast.LENGTH_SHORT).show();
                return;
            }

            int qty = order.containsKey(productId) ? order.get(productId) + 1 : 1;
            if (qty > p.getStock()) {
                Toast.makeText(this, "Not enough stock for " + p.getName(), Toast.LENGTH_SHORT).show();
                return;
            }
            order.put(productId, qty);
        } else {
            // drinks are made to order so don't enforce stock limits
            int qty = order.containsKey(productId) ? order.get(productId) + 1 : 1;
            order.put(productId, qty);
        }
        rebuildOrderView();
    }

    private void rebuildOrderView() {
        tvOrderList.removeAllViews();
        total = 0.0;
        if (order.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No items");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            empty.setPadding(8,8,8,8);
            tvOrderList.addView(empty);
        } else {
            for (Map.Entry<String, Integer> e : order.entrySet()) {
                Product p = productMap.get(e.getKey());
                if (p == null) continue;
                int q = e.getValue();

                // compute unit price taking discount into account
                double unitPrice = p.getPrice();
                if (activeDiscountPercent > 0) {
                    unitPrice = unitPrice * (1 - (activeDiscountPercent / 100.0));
                }
                double line = unitPrice * q;

                // create a horizontal container for the order line: label + small trash button
                LinearLayout lineContainer = new LinearLayout(this);
                lineContainer.setOrientation(LinearLayout.HORIZONTAL);
                lineContainer.setPadding(4,4,4,4);
                lineContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                TextView lineLabel = new TextView(this);
                lineLabel.setText(p.getName() + " x " + q + " -> ₱" + String.format(Locale.getDefault(), "%.2f", line));
                lineLabel.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                lineLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                lineLabel.setPadding(8,8,8,8);

                ImageButton removeBtn = new ImageButton(this);
                removeBtn.setImageResource(android.R.drawable.ic_menu_delete);
                removeBtn.setBackgroundColor(Color.TRANSPARENT);
                // Make the trash icon slightly smaller but still comfortably tappable
                int size = (int) (28 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(size, size);
                btnParams.setMargins(8,0,0,0);
                removeBtn.setLayoutParams(btnParams);
                removeBtn.setContentDescription("Remove " + p.getName());

                // when tapped, remove this product line from the order
                removeBtn.setOnClickListener(v -> {
                    order.remove(p.getId());
                    Toast.makeText(DashboardActivity.this, "Removed " + p.getName(), Toast.LENGTH_SHORT).show();
                    rebuildOrderView();
                });

                lineContainer.addView(lineLabel);
                lineContainer.addView(removeBtn);

                tvOrderList.addView(lineContainer);
                total += line;
            }
        }
        tvTotal.setText(String.format("Total: ₱%.2f", total));

        // If there are no items, ensure any removal mode is off and top button disabled (hidden)
        if (btnRemoveProduct != null) {
            if (order.isEmpty()) {
                removalMode = false;
                btnRemoveProduct.setEnabled(false);
            } else {
                btnRemoveProduct.setEnabled(false); // keep it disabled and hidden
            }
        }
    }

    private void refreshOrderListVisuals() {
        // kept for backwards compatibility but not used when per-line remove buttons are present
        for (int i = 0; i < tvOrderList.getChildCount(); i++) {
            View child = tvOrderList.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                Object tag = tv.getTag();
                if (tag == null) continue;
                String text = tv.getText().toString();
                if (removalMode) {
                    if (!text.startsWith("Tap to remove:")) {
                        tv.setTextColor(Color.RED);
                        tv.setText("Tap to remove: " + text);
                    }
                } else {
                    if (text.startsWith("Tap to remove: ")) {
                        String newText = text.replaceFirst("Tap to remove: ", "");
                        tv.setText(newText);
                        tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                    }
                }
            }
        }
    }

    private void doCheckout() {
        if (order.isEmpty()) {
            Toast.makeText(this, "No items in order", Toast.LENGTH_SHORT).show();
            return;
        }

        List<SaleItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> e : order.entrySet()) {
            Product p = productMap.get(e.getKey());
            if (p == null) continue;
            int q = e.getValue();

            // compute unit price taking discount into account
            double unitPrice = p.getPrice();
            if (activeDiscountPercent > 0) {
                unitPrice = unitPrice * (1 - (activeDiscountPercent / 100.0));
            }

            items.add(new SaleItem(p.getId(), p.getName(), q, unitPrice));
            // reduce stock only for non-drinks
            String cat = p.getCategory() == null ? "" : p.getCategory().toLowerCase(Locale.ROOT);
            boolean isDrink = cat.contains("drink");
            if (!isDrink) {
                p.setStock(p.getStock() - q);
            }
        }

        saveProducts();
        // after saving products and creating the sale, reset any active discount
        // so that subsequent product displays show original prices
        activeDiscountPercent = 0;
        activeDiscountCode = null;
        renderProducts();
        TextView tvAlerts = findViewById(R.id.tvAlerts);
        updateLowStockAlerts(tvAlerts);

        String saleId = "s_" + System.currentTimeMillis();
        Sale sale = new Sale(saleId, items, total, System.currentTimeMillis());
        saveSale(sale);

        printReceipt(sale);

        order.clear();
        rebuildOrderView();

        Toast.makeText(this, "Checkout complete", Toast.LENGTH_SHORT).show();
    }

    private void saveSale(Sale sale) {
        String json = prefs.getString(KEY_SALES, null);
        List<Sale> sales;
        if (json != null) {
            sales = gson.fromJson(json, new TypeToken<List<Sale>>(){}.getType());
            if (sales == null) sales = new ArrayList<>();
        } else {
            sales = new ArrayList<>();
        }
        sales.add(sale);
        prefs.edit().putString(KEY_SALES, gson.toJson(sales)).apply();
    }

    private void printReceipt(Sale sale) {
        StringBuilder html = new StringBuilder();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        html.append("<html><body style='font-family: sans-serif;'>");
        html.append("<h2>Cafelaria</h2>");
        html.append("<div>").append(df.format(new Date(sale.getTimestamp()))).append("</div>");
        html.append("<pre>");
        double subtotal = 0.0;
        for (SaleItem it : sale.getItems()) {
            double line = it.getPrice() * it.getQuantity();
            subtotal += line;
            html.append(it.getName()).append(" x").append(it.getQuantity()).append("  ₱").append(String.format(Locale.getDefault(), "%.2f", line)).append("\n");
        }
        html.append("\nTotal: ₱").append(String.format(Locale.getDefault(), "%.2f", sale.getTotal()));
        html.append("</pre>");
        html.append("<div>Thank you for your purchase!</div>");
        html.append("</body></html>");

        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                createWebPrintJob(view, sale.getId());
            }
        });
        webView.loadDataWithBaseURL(null, html.toString(), "text/HTML", "UTF-8", null);
    }

    private void createWebPrintJob(WebView webView, String jobName) {
        PrintManager printManager = (PrintManager) this.getSystemService(PRINT_SERVICE);
        if (printManager == null) return;
        PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);
        PrintAttributes attrs = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.NA_LETTER).setResolution(new PrintAttributes.Resolution("id", "id", 300, 300)).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();
        printManager.print(jobName, adapter, attrs);
    }

    // Discount dialog and helpers
    private void openDiscountDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_discount, null);
        EditText etCode = dialogView.findViewById(R.id.etDiscountCode);
        TextView tvPreview = dialogView.findViewById(R.id.tvDiscountPreview);
        Button b5 = dialogView.findViewById(R.id.btnDiscount5);
        Button b10 = dialogView.findViewById(R.id.btnDiscount10);
        Button b15 = dialogView.findViewById(R.id.btnDiscount15);
        Button bCancel = dialogView.findViewById(R.id.btnCancelDiscount);
        Button bApply = dialogView.findViewById(R.id.btnApplyDiscount);

        // preview percent starts with current active discount (if any)
        final int[] previewPercent = { activeDiscountPercent };

        // pick a sample price to show preview (first product in current view, or 0)
        final double[] samplePriceRef = new double[]{0.0};
        for (Product p : productMap.values()) {
            String cat = p.getCategory() == null ? "" : p.getCategory().toLowerCase(Locale.ROOT);
            boolean isDrink = cat.contains("drink");
            boolean isBakery = cat.contains("bakery") || cat.contains("pastry");
            if ("Drinks".equals(currentSection) && !isDrink) continue;
            if ("Pastries".equals(currentSection) && !isBakery) continue;
            samplePriceRef[0] = p.getPrice();
            break;
        }

        // update preview helper
        Runnable updatePreview = () -> runOnUiThread(() -> {
            if (previewPercent[0] > 0) {
                double discounted = samplePriceRef[0] * (1 - (previewPercent[0] / 100.0));
                tvPreview.setText(String.format(Locale.getDefault(), "Preview: ₱%.2f -> ₱%.2f (%d%% off)", samplePriceRef[0], discounted, previewPercent[0]));
            } else {
                tvPreview.setText("No discount selected — choose a percentage or type a code to preview.");
            }
        });

        updatePreview.run();

        // quick-select buttons
        b5.setOnClickListener(v -> {
            previewPercent[0] = 5; updatePreview.run();
        });
        b10.setOnClickListener(v -> {
            previewPercent[0] = 10; updatePreview.run();
        });
        b15.setOnClickListener(v -> {
            previewPercent[0] = 15; updatePreview.run();
        });

        // live code input: try to detect a number inside the code and use it as percent if it's 5/10/15
        etCode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String txt = s.toString();
                int found = 0;
                try {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(5|10|15)").matcher(txt);
                    if (m.find()) {
                        found = Integer.parseInt(m.group(1));
                    }
                } catch (Exception ex) { found = 0; }
                if (found == 5 || found == 10 || found == 15) {
                    previewPercent[0] = found; updatePreview.run();
                }
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        bCancel.setOnClickListener(v -> dialog.dismiss());
        bApply.setOnClickListener(v -> {
            // apply previewPercent as the active discount
            activeDiscountPercent = previewPercent[0];
            activeDiscountCode = etCode.getText().toString();
            renderProducts();
            dialog.dismiss();
            Toast.makeText(DashboardActivity.this, "Discount applied: " + activeDiscountPercent + "%", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}
