package jycessing.mode.export;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import jycessing.mode.PyEditor;
import processing.app.Base;
import processing.app.Library;
import processing.app.Preferences;
import processing.app.Sketch;
import processing.app.SketchCode;

/**
 * 
 * Subclass this to add more platforms, if we ever want to add freebsd or haiku or something
 *
 */
public abstract class PlatformExport {
  protected int id;
  protected String name;
  protected PyEditor editor;
  protected Sketch sketch;
  protected Set<Library> libraries;
  protected Arch arch;

  /**
   * Instance so that subclasses can override it.
   */
  protected abstract void log(final String msg);

  public abstract void export() throws IOException;

  /**
   * This is the same between platforms.
   * 
   */
  public void copyBasicStructure(final File destFolder) throws IOException {
    final boolean hasData = sketch.hasDataFolder();
    final boolean hasCode = sketch.hasCodeFolder();
    final boolean deletePrevious = Preferences.getBoolean("export.delete_target_folder");

    final File libFolder = new File(destFolder, "lib");
    final File codeFolder = new File(destFolder, "code");
    final File sourceFolder = new File(destFolder, "source");
    final File dataFolder = new File(destFolder, "data");
    final File jycessingFolder = new File(libFolder, "jycessing");

    // Delete previous export (if the user wants to, and it exists) and make a new one
    if (deletePrevious) {
      log("Removing old export folder.");
      Base.removeDir(destFolder);
    }
    destFolder.mkdirs();

    // Handle data folder
    if (hasData) {
      log("Copying data folder to export.");
      dataFolder.mkdirs();
      Base.copyDir(sketch.getDataFolder(), dataFolder);
    }

    // Handle code folder
    if (hasCode) {
      log("Copying code folder to export.");
      codeFolder.mkdirs();
      Base.copyDir(sketch.getCodeFolder(), codeFolder);
    }

    // Handle source folder
    {
      log("Copying source to export.");
      sourceFolder.mkdirs();
      for (final SketchCode code : sketch.getCode()) {
        code.copyTo(new File(sourceFolder, code.getFileName()));
      }
    }

    // Handle imported libraries
    {
      log("Copying libraries to export.");
      libFolder.mkdirs();
      for (final Library library : libraries) {
        final File libraryExportFolder =
            new File(libFolder, library.getFolder().getName() + "/library/");
        libraryExportFolder.mkdirs();
        for (final File exportFile : library.getApplicationExports(id, arch.bits)) {
          log("Exporting: " + exportFile);
          final String exportName = exportFile.getName();
          if (!exportFile.exists()) {
            System.err.println("The file " + exportName + " is mentioned in the export.txt from "
                + library + " but does not actually exist. Moving on.");
            continue;
          }
          if (exportFile.isDirectory()) {
            Base.copyDir(exportFile, new File(libraryExportFolder, exportName));
          } else {
            Base.copyFile(exportFile, new File(libraryExportFolder, exportName));
          }
        }
      }
    }

    // Handle Python Mode stuff
    {
      jycessingFolder.mkdirs();
      log("Copying core processing stuff to export");
      for (final File exportFile : new Library(Base.getContentFile("core")).getApplicationExports(
          id, arch.bits)) {
        if (exportFile.isDirectory()) {
          Base.copyDir(exportFile, new File(jycessingFolder, exportFile.getName()));
        } else {
          Base.copyFile(exportFile, new File(jycessingFolder, exportFile.getName()));
        }
      }
      log("Copying core processing.py .jars to export");
      Base.copyDir(editor.getModeContentFile("mode"), jycessingFolder);
      log("Copying splash screen to export");
      // (In the "lib" folder just in case the user has a splash.png)
      Base.copyFile(editor.getSplashFile(), new File(jycessingFolder, "splash.png"));
    }
  }
}
