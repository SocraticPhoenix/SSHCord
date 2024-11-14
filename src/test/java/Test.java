import com.socraticphoenix.sshcord.util.StringUtil;

public class Test {

    public static void main(String[] args) {
        String in = "\\u001b[A";
        String out = StringUtil.deEscape(in);

        System.out.println(out);
    }

}
