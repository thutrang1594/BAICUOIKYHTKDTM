package vn.tlu.cse.ht2.nhom16.moneymanagementapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.BuildConfig; // Đảm bảo import BuildConfig từ package chính xác của bạn

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Expense;

public class AiInsightsFragment extends Fragment {

    private static final String TAG = "AiInsightsFragment";

    private Button btnGenerateAiInsights, btnAutoBudget; // Thêm nút mới
    private TextView tvAiInsightsAnalysis, tvSpendingSummary, tvAdvice, tvImprovementAreas;
    private ProgressBar progressBarAiInsights;
    private CardView cardSpendingSummary, cardAdvice, cardImprovementAreas;

    private MainActivity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // Handler để cập nhật UI từ luồng khác

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
        } else {
            throw new RuntimeException(context.toString() + " must be MainActivity");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_insights, container, false);

        btnGenerateAiInsights = view.findViewById(R.id.btn_generate_ai_insights);
        btnAutoBudget = view.findViewById(R.id.btn_auto_budget); // Ánh xạ nút mới
        tvAiInsightsAnalysis = view.findViewById(R.id.tv_ai_insights_analysis);
        progressBarAiInsights = view.findViewById(R.id.progress_bar_ai_insights);
        tvSpendingSummary = view.findViewById(R.id.tv_spending_summary);
        tvAdvice = view.findViewById(R.id.tv_advice);
        tvImprovementAreas = view.findViewById(R.id.tv_improvement_areas);
        cardSpendingSummary = view.findViewById(R.id.card_spending_summary);
        cardAdvice = view.findViewById(R.id.card_advice);
        cardImprovementAreas = view.findViewById(R.id.card_improvement_areas);

        btnGenerateAiInsights.setOnClickListener(v -> generateAiInsights());

        // Lắng nghe sự kiện cho nút mới
        btnAutoBudget.setOnClickListener(v -> generateAutomaticBudget());


        // Ẩn tất cả các Card ban đầu
        cardSpendingSummary.setVisibility(View.GONE);
        cardAdvice.setVisibility(View.GONE);
        cardImprovementAreas.setVisibility(View.GONE);

        return view;
    }

    private void generateAiInsights() {
        if (activity == null || !isAdded()) {
            Toast.makeText(getContext(), "Ứng dụng chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
            return;
        }

        tvAiInsightsAnalysis.setText("Đang tạo gợi ý...");
        progressBarAiInsights.setVisibility(View.VISIBLE);
        btnGenerateAiInsights.setEnabled(false);
        btnAutoBudget.setEnabled(false);

        // Ẩn các gợi ý trước đó
        cardSpendingSummary.setVisibility(View.GONE);
        cardAdvice.setVisibility(View.GONE);
        cardImprovementAreas.setVisibility(View.GONE);
        tvSpendingSummary.setText("");
        tvAdvice.setText("");
        tvImprovementAreas.setText("");

        new Thread(() -> {
            String prompt = prepareAiPrompt();
            String jsonResponse = callGeminiApi(prompt);

            mainHandler.post(() -> {
                progressBarAiInsights.setVisibility(View.GONE);
                btnGenerateAiInsights.setEnabled(true);
                btnAutoBudget.setEnabled(true);
                if (jsonResponse != null && !jsonResponse.isEmpty()) {
                    displayInsights(jsonResponse);
                } else {
                    tvAiInsightsAnalysis.setText("Không thể tạo gợi ý chi tiêu lúc này. Vui lòng thử lại.");
                }
            });
        }).start();
    }

    private void generateAutomaticBudget() {
        if (activity == null || !isAdded()) {
            Toast.makeText(getContext(), "Ứng dụng chưa sẵn sàng.", Toast.LENGTH_SHORT).show();
            return;
        }

        tvAiInsightsAnalysis.setText("Đang tạo ngân sách tự động...");
        progressBarAiInsights.setVisibility(View.VISIBLE);
        btnGenerateAiInsights.setEnabled(false);
        btnAutoBudget.setEnabled(false);

        new Thread(() -> {
            String prompt = prepareAutomaticBudgetPrompt();
            String jsonResponse = callGeminiApi(prompt);

            mainHandler.post(() -> {
                progressBarAiInsights.setVisibility(View.GONE);
                btnGenerateAiInsights.setEnabled(true);
                btnAutoBudget.setEnabled(true);
                if (jsonResponse != null && !jsonResponse.isEmpty()) {
                    handleBudgetProposalResponse(jsonResponse);
                } else {
                    tvAiInsightsAnalysis.setText("Không thể tạo ngân sách lúc này. Vui lòng thử lại.");
                }
            });
        }).start();
    }

    private String prepareAutomaticBudgetPrompt() {
        List<Expense> expenses = activity.getExpenseList();
        DecimalFormat decimalFormat = activity.getDecimalFormat();
        String currency = activity.getCurrentCurrency();

        double totalIncome = 0;
        Map<String, Double> expenseCategoryData = new HashMap<>();
        for (Expense expense : expenses) {
            if (expense.getType().equals("income")) {
                totalIncome += expense.getAmount();
            } else if (expense.getType().equals("expense")) {
                String category = expense.getCategory();
                expenseCategoryData.put(category, expenseCategoryData.getOrDefault(category, 0.0) + expense.getAmount());
            }
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Bạn là một chuyên gia tư vấn tài chính. Dựa vào dữ liệu chi tiêu trong tháng qua, hãy đề xuất một kế hoạch ngân sách cho tháng tới. ");
        promptBuilder.append("Tổng thu nhập của người dùng là: ").append(decimalFormat.format(totalIncome)).append(" ").append(currency).append(". ");
        promptBuilder.append("Chi tiêu chi tiết theo danh mục: ");

        String expenseDetails = expenseCategoryData.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + decimalFormat.format(entry.getValue()) + " " + currency)
                .collect(Collectors.joining("; "));
        promptBuilder.append(expenseDetails).append(". ");

        promptBuilder.append("Hãy đưa ra một kế hoạch ngân sách hợp lý, đảm bảo tổng ngân sách không vượt quá tổng thu nhập. ");
        promptBuilder.append("Trả lời bằng tiếng Việt, dưới dạng một chuỗi JSON thuần túy (raw JSON string) chứa một mảng (array) các đối tượng, mỗi đối tượng có hai trường: 'category' (tên danh mục) và 'amount' (số tiền ngân sách). Ví dụ: ");
        promptBuilder.append("[{\"category\": \"Ăn uống\", \"amount\": 5000000}, {\"category\": \"Đi lại\", \"amount\": 1500000}]");

        return promptBuilder.toString();
    }


    private void handleBudgetProposalResponse(String rawJsonResponse) {
        try {
            String jsonToParse = extractJsonFromLlmResponse(rawJsonResponse);
            if (jsonToParse == null || jsonToParse.isEmpty()) {
                throw new JSONException("Không tìm thấy JSON hợp lệ trong phản hồi.");
            }

            final JSONArray budgetArray = new JSONArray(jsonToParse);

            StringBuilder proposalText = new StringBuilder("AI đề xuất kế hoạch ngân sách cho tháng tới:\n\n");
            for (int i = 0; i < budgetArray.length(); i++) {
                JSONObject budgetItem = budgetArray.getJSONObject(i);
                String category = budgetItem.getString("category");
                double amount = budgetItem.getDouble("amount");
                proposalText.append("• ").append(category).append(": ").append(activity.getDecimalFormat().format(amount)).append(" ").append(activity.getCurrentCurrency()).append("\n");
            }

            // Hiển thị kết quả trong một Dialog để người dùng xem
            new AlertDialog.Builder(requireContext())
                    .setTitle("Đề xuất Ngân sách Thông minh")
                    .setMessage(proposalText.toString())
                    .setPositiveButton("Lưu Ngân sách", (dialog, which) -> {
                        if (activity != null) {
                            activity.saveBudgetFromAI(budgetArray);
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();

        } catch (JSONException e) {
            Log.e(TAG, "Lỗi phân tích JSON đề xuất ngân sách. Phản hồi thô: " + rawJsonResponse, e);
            tvAiInsightsAnalysis.setText("Không thể đọc đề xuất ngân sách từ AI. Định dạng có thể không hợp lệ.");
        }
    }


    private String prepareAiPrompt() {
        List<Expense> expenses = activity.getExpenseList();
        DecimalFormat decimalFormat = activity.getDecimalFormat();
        String currency = activity.getCurrentCurrency();

        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> expenseCategoryData = new HashMap<>();

        for (Expense expense : expenses) {
            if (expense.getType().equals("income")) {
                totalIncome += expense.getAmount();
            } else if (expense.getType().equals("expense")) {
                totalExpense += expense.getAmount();
                String category = expense.getCategory();
                expenseCategoryData.put(category, expenseCategoryData.getOrDefault(category, 0.0) + expense.getAmount());
            }
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Phân tích thói quen chi tiêu sau đây và đưa ra lời khuyên để quản lý tài chính tốt hơn. ");
        promptBuilder.append("Tổng thu nhập: ").append(decimalFormat.format(totalIncome)).append(" ").append(currency).append(". ");
        promptBuilder.append("Tổng chi tiêu: ").append(decimalFormat.format(totalExpense)).append(" ").append(currency).append(". ");
        promptBuilder.append("Chi tiêu theo danh mục: ");

        if (expenseCategoryData.isEmpty()) {
            promptBuilder.append("Không có chi tiêu nào được ghi nhận.");
        } else {
            promptBuilder.append(
                    expenseCategoryData.entrySet().stream()
                            .map(entry -> entry.getKey() + ": " + decimalFormat.format(entry.getValue()) + " " + currency)
                            .collect(Collectors.joining("; "))
            ).append(".");
        }
        promptBuilder.append("\n\n");
        promptBuilder.append("Hãy trả lời bằng tiếng Việt, dưới dạng một đối tượng JSON với 3 trường: \"spending_summary\", \"advice\", và \"improvement_areas\". Mỗi trường này chứa một chuỗi văn bản, có thể bao gồm các dấu đầu dòng (gạch nối) để liệt kê các điểm. Ví dụ:\n")
                .append("{\n")
                .append("  \"spending_summary\": \"- Tóm tắt chi tiêu 1\\n- Tóm tắt chi tiêu 2\",\n")
                .append("  \"advice\": \"- Lời khuyên 1\\n- Lời khuyên 2\",\n")
                .append("  \"improvement_areas\": \"- Lĩnh vực cải thiện 1\\n- Lĩnh vực cải thiện 2\"\n")
                .append("}\n");


        return promptBuilder.toString();
    }

    private String callGeminiApi(String prompt) {
        String insight = null;
        HttpURLConnection conn = null; // Khởi tạo null ở đây
        try {
            String apiKey = BuildConfig.GEMINI_API_KEY;

            if ("YOUR_PLACEHOLDER_API_KEY".equals(apiKey)) {
                Log.e(TAG, "API Key is a placeholder. Please set your actual Gemini API key in local.properties.");
                mainHandler.post(() -> Toast.makeText(getContext(), "Lỗi: Chưa cấu hình API Key Gemini. Vui lòng kiểm tra file local.properties.", Toast.LENGTH_LONG).show());
                return null;
            }

            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            URL url = new URL(apiUrl);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject payload = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject userPart = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            partsArray.put(textPart);
            userPart.put("role", "user");
            userPart.put("parts", partsArray);
            contents.put(userPart);
            payload.put("contents", contents);

            Log.d(TAG, "Sending payload: " + payload.toString());

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }
                    Log.d(TAG, "Received raw response: " + response.toString());
                    insight = response.toString();
                }
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    Log.e(TAG, "Error response from API: " + errorResponse.toString());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calling Gemini API: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return insight;
    }

    private String extractJsonFromLlmResponse(String rawJsonResponse) {
        if (rawJsonResponse == null || rawJsonResponse.isEmpty()) {
            return null;
        }
        try {
            JSONObject outerJson = new JSONObject(rawJsonResponse);
            if (outerJson.has("candidates")) {
                JSONArray candidates = outerJson.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject candidate = candidates.getJSONObject(0);
                    if (candidate.has("content")) {
                        JSONObject content = candidate.getJSONObject("content");
                        if (content.has("parts") && content.getJSONArray("parts").length() > 0) {
                            String llmResponseText = content.getJSONArray("parts").getJSONObject(0).getString("text");
                            return extractJsonFromString(llmResponseText);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Phản hồi thô có thể không phải là định dạng LLM mong đợi. Thử coi nó là JSON trực tiếp.", e);
            return extractJsonFromString(rawJsonResponse); // Fallback
        }
        return null;
    }


    private void displayInsights(String rawJsonResponse) {
        try {
            String finalJsonToParse = extractJsonFromLlmResponse(rawJsonResponse);

            Log.d(TAG, "Final JSON string to parse after cleaning: '" + finalJsonToParse + "'");

            if (finalJsonToParse == null || finalJsonToParse.isEmpty()) {
                throw new JSONException("No valid JSON found after extraction and cleaning.");
            }

            JSONObject parsedInsights = new JSONObject(finalJsonToParse);


            String summary = parsedInsights.optString("spending_summary", "Không có tóm tắt.");
            String advice = parsedInsights.optString("advice", "Không có lời khuyên.");
            String improvements = parsedInsights.optString("improvement_areas", "Không có lĩnh vực cải thiện.");

            // Loại bỏ các ký tự ** từ các chuỗi trước khi hiển thị
            summary = removeMarkdownBold(summary);
            advice = removeMarkdownBold(advice);
            improvements = removeMarkdownBold(improvements);

            tvAiInsightsAnalysis.setText("Dưới đây là phân tích và gợi ý của AI:");
            tvSpendingSummary.setText(summary);
            tvAdvice.setText(advice);
            tvImprovementAreas.setText(improvements);

            cardSpendingSummary.setVisibility(View.VISIBLE);
            cardAdvice.setVisibility(View.VISIBLE);
            cardImprovementAreas.setVisibility(View.VISIBLE);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing AI insight JSON. Details: " + e.getMessage(), e);
            tvAiInsightsAnalysis.setText("Lỗi khi xử lý gợi ý từ AI. Định dạng không hợp lệ.");
            cardSpendingSummary.setVisibility(View.GONE);
            cardAdvice.setVisibility(View.GONE);
            cardImprovementAreas.setVisibility(View.GONE);
        } catch (Exception e) { // Bắt các lỗi không mong muốn khác trong quá trình hiển thị
            Log.e(TAG, "Unexpected error in displayInsights: " + e.getMessage(), e);
            tvAiInsightsAnalysis.setText("Có lỗi không xác định xảy ra khi hiển thị gợi ý.");
            cardSpendingSummary.setVisibility(View.GONE);
            cardAdvice.setVisibility(View.GONE);
            cardImprovementAreas.setVisibility(View.GONE);
        }
    }

    private String extractJsonFromString(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        input = input.trim();
        Log.d(TAG, "Input for extractJsonFromString after trim: '" + input + "'");


        Pattern markdownPattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.DOTALL);
        Matcher markdownMatcher = markdownPattern.matcher(input);
        if (markdownMatcher.find()) {
            String extracted = markdownMatcher.group(1).trim();
            Log.d(TAG, "Extracted by markdown pattern: '" + extracted + "'");
            return extracted;
        }

        int firstBrace = input.indexOf('{');
        int lastBrace = input.lastIndexOf('}');
        int firstBracket = input.indexOf('[');
        int lastBracket = input.lastIndexOf(']');

        // Check for array response
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            return input.substring(firstBracket, lastBracket + 1);
        }
        // Check for object response
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return input.substring(firstBrace, lastBrace + 1);
        }
        Log.d(TAG, "No JSON found in extractJsonFromString.");
        return null;
    }

    private String removeMarkdownBold(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace("**", "");
    }
}
