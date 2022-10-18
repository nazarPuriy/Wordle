
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class ClientAutomaticTest {
    ClientAutomatic ca;

    @Before
    public void SetUp(){
    }

    @Test
    public void testMatchesPattern() {

        //Tests fàcils on sí encaixa
        assert(ClientAutomatic.matchesReceivedPattern("ABCDE", "AEIOU", "^?***"));
        assert(ClientAutomatic.matchesReceivedPattern("ABCDE", "BCDEA", "?????"));

        //Tests fàcils on no encaixa
        assert(!ClientAutomatic.matchesReceivedPattern("ABCDE", "AEIOU", "^****"));
        assert(!ClientAutomatic.matchesReceivedPattern("ABCDE", "AEIOU", "^?*?*"));
        assert(!ClientAutomatic.matchesReceivedPattern("ABCDE", "AEIOU", "^?**?*"));

        //Comprovem que donem la oportunitat a que apareguin els caràcters repetits més vegades, sempre que no ens trobem el caràcter amg '*'
        assert(ClientAutomatic.matchesReceivedPattern("ABABA", "BABAB", "????*"));
        assert(ClientAutomatic.matchesReceivedPattern("ABABA", "BABAB", "??*??"));
        assert(ClientAutomatic.matchesReceivedPattern("ABABA", "BABAB", "*????"));

        //Aquí comprovem que no admetem paraules amb massa interrogants per alguna lletra, quan sí que hi ha '*' present
        assert(!ClientAutomatic.matchesReceivedPattern("ABABA", "BABAB", "?*???"));
        assert(!ClientAutomatic.matchesReceivedPattern("ABABA", "BABAB", "???*?"));

        //Aquí comprovem que no admetem paraules amb massa pocs d'un caràcter amb interrogants / coincidències
        assert(!ClientAutomatic.matchesReceivedPattern("ABABA", "BABAB", "?????"));
        assert(!ClientAutomatic.matchesReceivedPattern("ABABA", "BABBA", "???^^"));

    }

}
