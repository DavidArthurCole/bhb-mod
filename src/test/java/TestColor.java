import com.davidarthurcole.bhb.Color;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestColor {
    @Test
    public void testEquals(){

        Color red = new Color("red", "FFFF00", "");

        Color lightRed = new Color("lightred", "FFEE00", "lred");

        assertFalse(red.equals(lightRed));

        assertTrue(lightRed.equals("lred"));

    }
}
