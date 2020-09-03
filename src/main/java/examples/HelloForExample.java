package examples;

public class HelloForExample {
    public int sum(int[] numbers) {
        int sum = 0;
        for(int element : numbers) {
            sum += element;
        }
        return sum;
    }
}
