package com.example.myapplication;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.apache.commons.math3.optim.*;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import java.util.ArrayList;
import java.util.Collection;

public class MainActivity extends AppCompatActivity {
    private EditText budgetField, weightField;
    private Button calculateButton, previousResultButton;
    private ListView resultListView;
    private String[] foodNames = {"Chicken", "Beef", "Lamb", "Fries", "Rice"};
    private double[] proteinPerServing = {25, 27, 26, 2, 3};
    private double[] costPerServing = {14, 15, 16.5, 5, 6};
    private double[] caloriePerServing = {250, 270, 245, 400, 250};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        budgetField = findViewById(R.id.budgetField);
        weightField = findViewById(R.id.weightField);
        calculateButton = findViewById(R.id.calculateButton);
        previousResultButton = findViewById(R.id.previousResultButton);
        resultListView = findViewById(R.id.resultListView);

        calculateButton.setOnClickListener(v -> calculateOptimalDiet());
        previousResultButton.setOnClickListener(v -> showPreviousResults());
    }

    private void calculateOptimalDiet() {
        try {
            int budget = Integer.parseInt(budgetField.getText().toString());
            int weight = Integer.parseInt(weightField.getText().toString());

            // Set up optimization problem
            LinearObjectiveFunction objectiveFunction =
                    new LinearObjectiveFunction(proteinPerServing, 0);

            Collection<LinearConstraint> constraints = new ArrayList<>();
            double[] maxFoods = new double[foodNames.length];

            // Add constraints
            for (int i = 0; i < foodNames.length; i++) {
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
        String[] results = new String[foodNames.length];
        for (int i = 0; i < foodNames.length; i++) {
            results[i] = String.format("%s: %.2f servings (Cost: $%.2f, Protein: %.1fg)",
                    foodNames[i],
                    amounts[i],
                    costPerServing[i] * amounts[i],
                    proteinPerServing[i] * amounts[i]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                results
        );
        resultListView.setAdapter(adapter);

        // Save results
        saveResults(amounts);
    }

    private void saveResults(double[] amounts) {
        // Convert to string representation for simple storage
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < amounts.length; i++) {
            sb.append(foodNames[i]).append(":").append(amounts[i]).append(";");
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
            Toast.makeText(this, "No previous results", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = saved.split(";");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                items
        );
        resultListView.setAdapter(adapter);
    }
}