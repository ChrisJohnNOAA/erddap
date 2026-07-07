import java.util.regex.*;

public class CodeQLRepro8 {
    public static void main(String[] args) {
        String regex = "^\\?(?:\"(?>[^\"\\\\\\n]*+(?:\\\\.[^\"\\\\\n]*+)*+\"|)|%22(?>[^%]*+(?:%(?!22)[^%]*+)*+%22|)|[^\\s#\"%.,?!#]++|[.,?!#]|%(?!22))*(?<![.,?!#])";
        Pattern p = Pattern.compile(regex);

        for (int i = 10; i <= 25; i++) {
            String input = "?%22" + "%22%22".repeat(i) + ".";

            long start = System.nanoTime();
            Matcher m = p.matcher(input);
            boolean found = m.find();
            long end = System.nanoTime();
            System.out.printf("I: %2d | Time: %8.3f ms | Match: %s | Result length: %d\n",
                i, (end - start) / 1000000.0, found, found ? m.group(0).length() : -1);
        }

        // Test correct matching of unclosed %22
        String unclosed = "?%22abc";
        Matcher m = p.matcher(unclosed);
        if (m.find()) {
            System.out.println("Unclosed %22: [" + m.group(0) + "]");
        }

        // Test correct matching of balanced %22
        String balanced = "?%22abc%22";
        m = p.matcher(balanced);
        if (m.find()) {
            System.out.println("Balanced %22: [" + m.group(0) + "]");
        }

        // Test % followed by something else
        String percent = "?a=b%21";
        m = p.matcher(percent);
        if (m.find()) {
            System.out.println("Percent: [" + m.group(0) + "]");
        }
    }
}
