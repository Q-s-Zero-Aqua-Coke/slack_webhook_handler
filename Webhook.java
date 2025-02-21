import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Webhook {
    public static void main(String[] args) {
        String prompt = System.getenv("LLM_PROMPT");
        String llmResult = useLLM(prompt);

        String template = System.getenv("LLM_IMAGE_TEMPLATE");
        String llmImageResult = useLLMForImage(template.formatted(llmResult));
        sendSlackMessage(llmResult, llmImageResult);
    }

    // 이미지 생성
    public static String useLLMForImage(String prompt){
        String apiUrl = System.getenv("LLM_IMG_API_URL");
        String apiKey = System.getenv("LLM_IMG_API_KEY");
        String model = System.getenv("LLM_IMG_MODEL");
        String payload = """
                {
                  "prompt": "%s",
                  "model": "%s",
                  "width": 1024,
                  "height": 768,
                  "steps": 2,
                  "n": 1
                }
                """.formatted(prompt, model);
        // 새롭게 요청한 클라이언트 생성
        HttpClient client = HttpClient.newHttpClient();
        // fetch
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        String result = null;

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body()
                    .split("url\": \"")[1]
                    .split("\",")[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    // LLM 사용
    public static String useLLM(String prompt){
        String apiUrl = System.getenv("LLM_API_URL");
        String apiKey = System.getenv("LLM_API_KEY");
        String model = System.getenv("LLM_MODEL");

        String payload = """
                {
                    "messages": [
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ],
                    "model": "%s"
                }
                """.formatted(prompt, model);

        // 새롭게 요청한 클라이언트 생성
        HttpClient client = HttpClient.newHttpClient();
        // fetch
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        String result = null;

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body()
                    .split("\"content\":\"")[1]
                    .split("\"},\"logprobs\"")[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return result;
    }

    public static void sendSlackMessage(String text, String imageUrl){
        String slackUrl = System.getenv("SLACK_WEBHOOK_URL");
        String payload = """
                {"attachments": [{
                    "text": "%s",
                    "image_url": "%s"
                }]} 
                """.formatted(text, imageUrl);
        System.out.println("payload = " + payload);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(slackUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
