package sample.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.repository.RepositorySystem;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "touch", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class MyMojo extends AbstractMojo {
    @Component
    private RepositorySystem repoSystem;

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(property = "targetArtifact", required = true)
    private Map<String, String> targetArtifact;

    public void execute() throws MojoExecutionException {
        String gId = targetArtifact.get("groupId");
        String aId = targetArtifact.get("artifactId");
        String ver = targetArtifact.get("version");
        String pkg = targetArtifact.get("packaging");

        Artifact art = repoSystem.createArtifact(gId, aId, ver, pkg);
        Artifact artSrces = repoSystem.createArtifactWithClassifier(gId, aId, ver, pkg, "sources");
        try {
            ArtifactRepository localRepo = repoSystem.createDefaultLocalRepository();
            art = localRepo.find(art);
            artSrces = localRepo.find(artSrces);
            getLog().info("Archive path:" + art.getFile().getAbsolutePath());
            getLog().info("src Path:" + artSrces.getFile().getAbsolutePath());
        } catch (InvalidRepositoryException e) {
            throw new MojoExecutionException("Error get Artifact. ", e);
        }

        File dir = outputDirectory;

        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if(file.isDirectory()) {
                    try {
                        FileUtils.forceDelete(file);
                    } catch (IOException e) {
                        getLog().warn("出力先のクリーンアップに失敗。", e);
                        e.printStackTrace();
                    }
                    
                }
            }
        }
        else {
            dir.mkdirs();
        }

        try (ZipFile zipFile = new ZipFile(artSrces.getFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    Path path = Paths.get(dir.getAbsolutePath(), entry.getName());
                    FileUtils.copyInputStreamToFile(zipFile.getInputStream(entry), path.toFile());
                }
            }
            Path path = Paths.get(dir.getAbsolutePath(), art.getFile().getName());
            FileUtils.copyFile(art.getFile(), path.toFile());
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating file.", e);
        } finally {
        }
    }

}
