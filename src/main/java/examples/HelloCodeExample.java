package examples;

import java.io.IOException;

public class HelloCodeExample {
    public static void main(String[] args) {
        try {
            foo();
        } catch(NullPointerException e) {
            System.out.println(e);
        } catch(IOException e) {
            System.out.println(e);
        }

        try {
            foo();
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public static void foo() throws IOException {
    }
}
