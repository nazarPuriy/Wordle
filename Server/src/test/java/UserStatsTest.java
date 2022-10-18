import org.json.simple.JSONObject;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;


public class UserStatsTest {

    UserStats userStats;


    @Before
    public void SetUp(){
        userStats = new UserStats();
    }


    @Test
    public void JSONTest() {

        JSONObject p = userStats.getJSON();
        assert(p.containsKey("Stats"));
        JSONObject q = (JSONObject)p.get("Stats");
        assert(q.containsKey("Jugades"));
        assert(q.containsKey("Exits"));
        assert(q.containsKey("Ratxa Actual"));
        assert(q.containsKey("Ratxa Maxima"));
        assert(q.containsKey("Victories"));
        JSONObject r = (JSONObject)q.get("Victories");
        assert(r.containsKey("1"));
        assert(r.containsKey("2"));
        assert(r.containsKey("3"));
        assert(r.containsKey("4"));
        assert(r.containsKey("5"));
        assert(r.containsKey("6"));

    }


    @Test
    public void addLossTest(){

        userStats = new UserStats();
        userStats.addLoss();
        userStats.addLoss();
        userStats.addLoss();
        userStats.addLoss();
        JSONObject p = userStats.getJSON();
        JSONObject q = (JSONObject)p.get("Stats");
        assertEquals(Integer.parseInt(q.get("Jugades").toString()), 4);
        assertEquals(q.get("Exits").toString(), "0.0");

    }


    @Test
    public void addWinTest(){

        userStats = new UserStats();
        userStats.addWin(1);
        userStats.addWin(2);
        userStats.addWin(3);
        userStats.addWin(4);
        JSONObject p = userStats.getJSON();
        JSONObject q = (JSONObject)p.get("Stats");
        JSONObject r = (JSONObject)q.get("Victories");
        assertEquals(Integer.parseInt(r.get("1").toString()), 1);
        assertEquals(Integer.parseInt(r.get("2").toString()), 1);
        assertEquals(Integer.parseInt(r.get("3").toString()), 1);
        assertEquals(Integer.parseInt(r.get("4").toString()), 1);
        assertEquals(Integer.parseInt(r.get("5").toString()), 0);
        assertEquals(Integer.parseInt(r.get("6").toString()), 0);
        assertEquals(Integer.parseInt(q.get("Jugades").toString()), 4);
        assertEquals(q.get("Exits").toString(), "100.0");

    }


    @Test
    public void exitsGeneralTest(){

        userStats = new UserStats();
        userStats.addWin(1);
        userStats.addWin(2);
        userStats.addLoss();
        JSONObject p = userStats.getJSON();
        JSONObject q = (JSONObject)p.get("Stats");
        JSONObject r = (JSONObject)q.get("Victories");
        assertEquals(Integer.parseInt(r.get("1").toString()), 1);
        assertEquals(Integer.parseInt(r.get("2").toString()), 1);
        assertEquals(Integer.parseInt(r.get("3").toString()), 0);
        assertEquals(Integer.parseInt(r.get("4").toString()), 0);
        assertEquals(Integer.parseInt(r.get("5").toString()), 0);
        assertEquals(Integer.parseInt(r.get("6").toString()), 0);
        assertEquals(Integer.parseInt(q.get("Jugades").toString()), 3);
        assertEquals(q.get("Exits").toString(), "66.66667");

    }

}

