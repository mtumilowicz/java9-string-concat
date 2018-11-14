import org.junit.Test;

/**
 * Created by mtumilowicz on 2018-11-14.
 */
public class StringConcatBenchmarkTest {
    
    @Test
    public void nonLoopConcatenation() {
        String a = "a";
        String b = "b";
        System.out.println(a + b);
    }

    @Test
    public void nonLoopConcatenation_usingStringBuilder() {
        String a = "a";
        String b = "b";
        
        System.out.println(new StringBuilder().append(a).append(b));
    }
    
    @Test
    public void loopConcatenation() {
        long start = System.currentTimeMillis();
        
        String result = "";
        
        for (int i = 0; i < 50_000; i++) {
            result += i;
        }

        System.out.println(result);

        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void loopConcatenation_usingStringBuilder() {
        long start = System.currentTimeMillis();

        StringBuilder result = new StringBuilder(50_000);

        for (int i = 0; i < 50_000; i++) {
            result.append(i);
        }

        System.out.println(result);

        System.out.println(System.currentTimeMillis() - start);
    }
}
