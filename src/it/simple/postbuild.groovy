import org.junit.Assert;
import java.nio.file.Files;
import java.nio.file.Paths;

Assert.assertTrue(Files.exists(Paths.get("target", "it-repo-results", "simple", "com", "kerbaya", "reposync", "simple", "1.0.0", "simple-1.0.0.jar")));
