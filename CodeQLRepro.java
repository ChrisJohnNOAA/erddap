import java.util.regex.*;

public class CodeQLRepro {
    public static void main(String[] args) {
        // U = unit of characters that are not boundary punctuation
        // P = boundary punctuation [.,?!]
        // Regex = ^\? U* (P+ U+)*

        String u = "(?:[^\\s#\"%.,?!]++|\"(?>(?:\\\\.|[^\"\\\\\\n])*+\"|)|%22(?>(?:(?!%22).)*+%22|)|%(?!22))";
        String p = "[.,?!]++";
        String regex = "^\\?" + u + "*(?:" + p + u + "+)*";

        System.out.println("Regex: " + regex);
        Pattern pattern = Pattern.compile(regex);

        String[] inputs = {
            "?a=b",
            "?a=\"q\"",
            "?a=\"unclosed",
            "?a=\"escaped\\\"\"",
            "?a=%22encoded%22",
            "?a=%22unclosed",
            "?%22%22%22%22%22%22",
            "?%22%22%22",
            "?a=%21",
            "?a=1.",
            "?a=\"1.2.3.\"",
            "?a#fragment",
            "?a=\"b c\"#d",
            "?.a",
            "??a",
            "?a..b",
            "?a...b...",
            "?a=\"q1\" \"q2\"",
            "?a=\"q1\""
        };

        for (String input : inputs) {
            Matcher m = pattern.matcher(input);
            boolean found = m.find();
            System.out.printf("  Input: %-30s Found: %-5s Match: %-20s\n",
                input, found, found ? m.group() : "N/A");
        }

        // Malicious Performance Check
        System.out.println("\nTesting malicious inputs:");

        // repetitions of ""
        StringBuilder sb = new StringBuilder("?");
        for (int i = 0; i < 1000; i++) sb.append("\"\"");
        sb.append("."); // Trailing punctuation

        long start = System.nanoTime();
        Matcher m = pattern.matcher(sb.toString());
        boolean found = m.find();
        long end = System.nanoTime();
        System.out.printf("Malicious \"\" (len %d) Time: %.3f ms. Match len: %d\n",
            sb.length(), (end - start) / 1e6, found ? m.group().length() : 0);

        // repetitions of %22%22
        sb = new StringBuilder("?");
        for (int i = 0; i < 1000; i++) sb.append("%22%22");
        sb.append(".");

        start = System.nanoTime();
        m = pattern.matcher(sb.toString());
        found = m.find();
        end = System.nanoTime();
        System.out.printf("Malicious %%22%%22 (len %d) Time: %.3f ms. Match len: %d\n",
            sb.length(), (end - start) / 1e6, found ? m.group().length() : 0);

        // repetitions of $
        sb = new StringBuilder("?");
        for (int i = 0; i < 2000; i++) sb.append("$");
        sb.append(".");

        start = System.nanoTime();
        m = pattern.matcher(sb.toString());
        found = m.find();
        end = System.nanoTime();
        System.out.printf("Malicious $ (len %d) Time: %.3f ms. Match len: %d\n",
            sb.length(), (end - start) / 1e6, found ? m.group().length() : 0);
    }
}
