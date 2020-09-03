package client;

import annotationprocessing.MyData;
import annotationprocessing.ToString;

@ToString
@MyData
public class User {
    private Long id;
    private String name;

    public void example(User this) {
        System.out.println("in example()");
        this.run(null);
    }

    //'example()' is already defined in 'client.User'
//    public void example() {
//
//    }

    public static void main(String[] args) {
        User user = new User();
        user.setId(1L);
        user.setName("John Doe");
        System.out.println("id: " + user.getId() + ", name: " + user.getName());
        System.out.println(user.toString());
        user.example();
    }
}
