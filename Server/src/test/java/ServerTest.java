import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;


public class ServerTest {

    Server server;


    @Before
    public void SetUp() {
        server = new Server();
    }


    @Test
    public void MatchStringTest() {
        assertEquals("****^",server.matchString("JUGAR", "RRRRR"));
        assertEquals("????*",server.matchString("ABABB", "BABAA"));
        assertEquals("*****",server.matchString("ASDFG", "LOPIU"));
    }

}