package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MainActivity extends BottomNavigationActivity {
    private EditText budgetField, weightField;
    private Button calculateButton, previousResultButton, addItemButton, viewFoodsButton, deleteItemButton;
    private ListView resultListView;
    private List<String> foodNamesList = new ArrayList<>(Arrays.asList("Chicken", "Beef", "Lamb", "Fries", "Rice"));
    private List<Double> proteinPerServingList = new ArrayList<>(Arrays.asList(25.0, 27.0, 26.0, 2.0, 3.0));
    private List<Double> costPerServingList = new ArrayList<>(Arrays.asList(14.0, 15.0, 16.5, 5.0, 6.0));
    private List<Double> caloriePerServingList = new ArrayList<>(Arrays.asList(250.0, 270.0, 245.0, 400.0, 250.0));

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("hello world");
        // Setup bottom navigation
        setupBottomNavigation();

        // Initialize views
        budgetField = findViewById(R.id.budgetField);
        weightField = findViewById(R.id.weightField);
        calculateButton = findViewById(R.id.calculateButton);
        previousResultButton = findViewById(R.id.previousResultButton);
        addItemButton = findViewById(R.id.addItemButton);
        viewFoodsButton = findViewById(R.id.viewFoodsButton);
        deleteItemButton = findViewById(R.id.deleteItemButton);
        resultListView = findViewById(R.id.resultListView);

        // Load saved food items
        loadSavedFoodItems();

        // Show all food items immediately on launch
        refreshFoodListView();

        // Then check for previous results
        showPreviousResults();

        // Set click listeners
        calculateButton.setOnClickListener(v -> calculateOptimalDiet());
        previousResultButton.setOnClickListener(v -> showPreviousResults());
        addItemButton.setOnClickListener(v -> showAddItemDialog());
        viewFoodsButton.setOnClickListener(v -> showFoodList());
        deleteItemButton.setOnClickListener(v -> showDeleteFoodDialog());
    }

    private void loadSavedFoodItems() {
        // Clear any previously loaded custom items (but keep the default 5)
        while (foodNamesList.size() > 5) {
            foodNamesList.remove(5);
            proteinPerServingList.remove(5);
            costPerServingList.remove(5);
            caloriePerServingList.remove(5);
        }

        String savedItems = getSharedPreferences("FoodItems", MODE_PRIVATE)
                .getString("items", "");

        if (!savedItems.isEmpty()) {
            String[] items = savedItems.split(";");
            for (String item : items) {
                String[] parts = item.split("\\|");
                if (parts.length == 4) {
                    try {
                        foodNamesList.add(parts[0]);
                        proteinPerServingList.add(Double.parseDouble(parts[1]));
                        costPerServingList.add(Double.parseDouble(parts[2]));
                        caloriePerServingList.add(Double.parseDouble(parts[3]));
                    } catch (NumberFormatException e) {
                        // Skip invalid entries
                    }
                }
            }
        }
    }

    private void showAddItemDialog() {
        // We'll use an inline input method - no separate XML needed
        final LinearLayout inputLayout = new LinearLayout(this);
        inputLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(android.R.dimen.app_icon_size) / 2;
        inputLayout.setPadding(padding, padding, padding, padding);

        // Create input fields
        final EditText nameInput = new EditText(this);
        nameInput.setHint("Food Name");

        final EditText proteinInput = new EditText(this);
        proteinInput.setHint("Protein per Serving (g)");
        proteinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        final EditText costInput = new EditText(this);
        costInput.setHint("Cost per Serving ($)");
        costInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        final EditText caloriesInput = new EditText(this);
        caloriesInput.setHint("Calories per Serving");
        caloriesInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        // Add views to parent layout
        inputLayout.addView(nameInput);
        inputLayout.addView(proteinInput);
        inputLayout.addView(costInput);
        inputLayout.addView(caloriesInput);

        // Add margins between elements
        for (int i = 0; i < inputLayout.getChildCount(); i++) {
            View child = inputLayout.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
            params.bottomMargin = padding / 2;
            child.setLayoutParams(params);
        }

        // Create and show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Food Item");
        builder.setView(inputLayout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            try {
                String name = nameInput.getText().toString().trim();
                double protein = Double.parseDouble(proteinInput.getText().toString());
                double cost = Double.parseDouble(costInput.getText().toString());
                double calories = Double.parseDouble(caloriesInput.getText().toString());

                if (name.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Food name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Add the new food item
                foodNamesList.add(name);
                proteinPerServingList.add(protein);
                costPerServingList.add(cost);
                caloriePerServingList.add(calories);

                // Save the updated food items
                saveFoodItems();

                // Refresh the ListView
                refreshFoodListView();

                Toast.makeText(MainActivity.this, "Food item added successfully", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveFoodItems() {
        StringBuilder sb = new StringBuilder();
        // Skip the first 5 default items when saving
        for (int i = 5; i < foodNamesList.size(); i++) {
            sb.append(foodNamesList.get(i)).append("|")
                    .append(proteinPerServingList.get(i)).append("|")
                    .append(costPerServingList.get(i)).append("|")
                    .append(caloriePerServingList.get(i)).append(";");
        }

        getSharedPreferences("FoodItems", MODE_PRIVATE)
                .edit()
                .putString("items", sb.toString())
                .apply();
        showPreviousResults();
    }

    private void showFoodList() {
        // Create a list of food items with their details
        String[] foodDetails = new String[foodNamesList.size()];
        for (int i = 0; i < foodNamesList.size(); i++) {
            foodDetails[i] = String.format("%s (Protein: %.1fg, Cost: $%.2f, Calories: %.0f)",
                    foodNamesList.get(i),
                    proteinPerServingList.get(i),
                    costPerServingList.get(i),
                    caloriePerServingList.get(i));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Available Foods");
        builder.setItems(foodDetails, null);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showDeleteFoodDialog() {
        if (foodNamesList.size() <= 5) {
            Toast.makeText(this, "No custom food items to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] customFoods = new String[foodNamesList.size() - 5];
        for (int i = 5; i < foodNamesList.size(); i++) {
            customFoods[i - 5] = foodNamesList.get(i);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Food Item");
        builder.setItems(customFoods, (dialog, which) -> {
            final int itemToDelete = which + 5;

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to delete " + foodNamesList.get(itemToDelete) + "?")
                    .setPositiveButton("Yes", (dialogInterface, i) -> {
                        foodNamesList.remove(itemToDelete);
                        proteinPerServingList.remove(itemToDelete);
                        costPerServingList.remove(itemToDelete);
                        caloriePerServingList.remove(itemToDelete);

                        saveFoodItems();
                        refreshFoodListView();

                        Toast.makeText(MainActivity.this, "Food item deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected int getNavigationMenuItemId() {
        // Return the ID of the Diet menu item to highlight it in navigation
        return R.id.navigation_diet;
    }

    private void calculateOptimalDiet() {
        try {
            int budget = Integer.parseInt(budgetField.getText().toString());
            int weight = Integer.parseInt(weightField.getText().toString());

            // Convert lists to arrays for optimization
            double[] proteinPerServing = new double[foodNamesList.size()];
            double[] costPerServing = new double[foodNamesList.size()];
            double[] caloriePerServing = new double[foodNamesList.size()];

            for (int i = 0; i < foodNamesList.size(); i++) {
                proteinPerServing[i] = proteinPerServingList.get(i);
                costPerServing[i] = costPerServingList.get(i);
                caloriePerServing[i] = caloriePerServingList.get(i);
            }

            // Set up optimization problem
            LinearObjectiveFunction objectiveFunction =
                    new LinearObjectiveFunction(proteinPerServing, 0);

            Collection<LinearConstraint> constraints = new ArrayList<>();
            double[] maxFoods = new double[foodNamesList.size()];

            // Add constraints
            for (int i = 0; i < foodNamesList.size(); i++) {
                maxFoods[i] = 1;
                constraints.add(new LinearConstraint(maxFoods, Relationship.LEQ, 3));
                maxFoods[i] = 0;
            }

            constraints.add(new LinearConstraint(caloriePerServing, Relationship.GEQ, weight*5));
            constraints.add(new LinearConstraint(caloriePerServing, Relationship.LEQ, weight*8));
            constraints.add(new LinearConstraint(costPerServing, Relationship.LEQ, budget));

            // Solve
            PointValuePair solution = new SimplexSolver().optimize(
                    new LinearConstraintSet(constraints),
                    objectiveFunction,
                    GoalType.MAXIMIZE,
                    new NonNegativeConstraint(true)
            );

            if (solution != null) {
                displayResults(solution.getPoint(), budget, weight);
            } else {
                Toast.makeText(this, "No feasible solution", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayResults(double[] amounts, int budget, int weight) {
        String[] results = new String[foodNamesList.size()];
        for (int i = 0; i < foodNamesList.size(); i++) {
            results[i] = String.format("%s: %.2f servings (Cost: $%.2f, Protein: %.1fg)",
                    foodNamesList.get(i),
                    amounts[i],
                    costPerServingList.get(i) * amounts[i],
                    proteinPerServingList.get(i) * amounts[i],
                    caloriePerServingList.get(i)*amounts[i]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                results
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setPadding(
                        dpToPx(8),  // left
                        dpToPx(8),  // top
                        dpToPx(8),  // right
                        dpToPx(8)   // bottom
                );

                return view;
            }

            private int dpToPx(int dp) {
                return (int) (dp * getResources().getDisplayMetrics().density);
            }
        };

        resultListView.setAdapter(adapter);
        saveResults(amounts);
    }

    private void saveResults(double[] amounts) {
        // Convert to string representation for simple storage
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < amounts.length; i++) {
            sb.append(foodNamesList.get(i)).append(":").append(amounts[i]).append(";");
        }

        getSharedPreferences("DietResults", MODE_PRIVATE)
                .edit()
                .putString("lastResults", sb.toString())
                .apply();
    }

    private void showPreviousResults() {
        String saved = getSharedPreferences("DietResults", MODE_PRIVATE)
                .getString("lastResults", "");

        if (saved.isEmpty()) {
            String[] emptyResults = new String[foodNamesList.size()];
            for (int i = 0; i < foodNamesList.size(); i++) {
                emptyResults[i] = foodNamesList.get(i) + ": 0.00 servings (Cost: $0.00, Protein: 0.0g)";
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    emptyResults
            );
            resultListView.setAdapter(adapter);
            return;
        }

        // Filter out results for deleted items
        List<String> validResults = new ArrayList<>();
        String[] items = saved.split(";");
        for (String item : items) {
            String foodName = item.split(":")[0];
            if (foodNamesList.contains(foodName)) {
                validResults.add(item);
            }
        }

        // If no valid results, show empty state
        if (validResults.isEmpty()) {
            validResults.add("No previous results available");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                validResults
        );
        resultListView.setAdapter(adapter);
    }
    private void refreshFoodListView() {
        String[] displayItems = new String[foodNamesList.size()];
        for (int i = 0; i < foodNamesList.size(); i++) {
            displayItems[i] = String.format("%s: 0.00 servings (Cost: $0.00, Protein: 0.0g, Calories: 0)",
                    foodNamesList.get(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayItems
        );
        resultListView.setAdapter(adapter);
    }
}