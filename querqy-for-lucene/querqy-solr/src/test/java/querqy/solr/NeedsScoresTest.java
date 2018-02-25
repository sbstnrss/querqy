package querqy.solr;


import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QueryParsing;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class NeedsScoresTest extends SolrTestCaseJ4 {

    public void index() throws Exception {

        assertU(adoc("id", "1", "f1", "qup"));
        assertU(adoc("id", "2", "f1", "qup other", "f2", "u100"));
        assertU(commit());
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        initCore("solrconfig-commonrules.xml", "schema.xml");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearIndex();
        index();
    }

    @Test
    public void testBoostIsAddedByDefault() throws Exception {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Boost not added by default",
                req,
                "//result[@name='response'][@numFound='2']",
                // the parsed query must contain the boost terms:
                "//str[@name='parsedquery'][contains(.,'f1:u100')]",
                "//str[@name='parsedquery'][contains(.,'f2:u100')]",
                "//str[@name='parsedquery'][not(contains(.,'ConstantScore'))]");
        req.close();
    }

    @Test
    public void testBoostIsAddedForNeedsScoresTrue() throws Exception {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                QuerqyDismaxQParser.NEEDS_SCORES, "true",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Boost not added",
                req,
                "//result[@name='response'][@numFound='2']",
                // the parsed query must contain the boost terms:
                "//str[@name='parsedquery'][contains(.,'f1:u100')]",
                "//str[@name='parsedquery'][contains(.,'f2:u100')]",
                "//str[@name='parsedquery'][not(contains(.,'ConstantScore'))]");
        req.close();
    }


    @Test
    public void testBoostIsNotAddedIfScoresAreNotNeeded() throws Exception {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.PF, "f1",
                QueryParsing.OP, "OR",
                QuerqyDismaxQParser.NEEDS_SCORES, "false",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Boost added for needsScores=false",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='parsedquery'][not(contains(.,'f1:u100'))]",
                "//str[@name='parsedquery'][not(contains(.,'f2:u100'))]");
        req.close();
    }

    @Test
    public void testQueryIsConstantScoreIfScoresAreNotNeeded() throws Exception {
        String q = "qup";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                QueryParsing.OP, "OR",
                QuerqyDismaxQParser.NEEDS_SCORES, "false",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("Not a ConstantScoreQuery",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='parsedquery'][contains(.,'ConstantScore')]");
        req.close();
    }

    @Test
    public void testQueryDoesNotContainPhraseQueryBoostIfScoresAreNotNeeded() throws Exception {
        String q = "qup s";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.PF, "f1",
                QueryParsing.OP, "OR",
                QuerqyDismaxQParser.NEEDS_SCORES, "false",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("PhraseQuery must not be added",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='parsedquery'][not(contains(.,'PhraseQuery'))]");
        req.close();
    }


    @Test
    public void testQueryContainsPhraseQueryBoostIfScoresAreNeeded() throws Exception {
        String q = "qup s";

        SolrQueryRequest req = req("q", q,
                DisMaxParams.QF, "f1 f2",
                DisMaxParams.PF, "f1",
                QueryParsing.OP, "OR",
                QuerqyDismaxQParser.NEEDS_SCORES, "true",
                "defType", "querqy",
                "debugQuery", "true"
        );

        assertQ("PhraseQuery must be added",
                req,
                "//result[@name='response'][@numFound='2']",
                "//str[@name='parsedquery'][contains(.,'PhraseQuery')]");
        req.close();
    }


}