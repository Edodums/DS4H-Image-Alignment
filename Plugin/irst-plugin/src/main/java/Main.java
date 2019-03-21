/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */


import ij.*;
import ij.io.OpenDialog;

import ij.process.ImageProcessor;

import java.io.IOException;
import loci.formats.ChannelSeparator;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ImageProcessorReader;
import loci.plugins.util.LociPrefs;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Plugin;

import net.imagej.ImageJ;

/** Loads and displays a dataset using the ImageJ API. */
@Plugin(type = Command.class, headless = true,
		menuPath = "Plugins>IRST")
public class Main implements Command, Previewable {


	public static void main(final String... args) throws Exception {

		final ImageJ ij = new ImageJ();
		ij.launch(args);

		ij.command().run(Main.class, true);
	}

	@Override
	public void run() {
		// Chiediamo come prima cosa il file all'utente
		String pathFile =  chooseInitialFile();

		ImporterOptions options;
		ImagePlus[] imps = null;
		try {
			options = generateIRSTOptions(pathFile);
			imps = BF.openImagePlus(options);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Da indagare. fa un "flatting" di canali multiimmagine. Utile in alcuni casi limite, ma quali sono le sue implicazioni?
		imps[0].flattenStack();

		MainDialog dialog = new MainDialog(imps[0], pathFile);
		dialog.show();
		if(dialog.dialog.wasCanceled())
			System.exit(0);

		// TODO: perché imps sempre 0?
		// Se l'utente vuole aprire l'immagine come multistack, deleghiamo tutto a imagej
		if(dialog.isOpenAsMultiStack()) {
			imps[0].show();
			return;
		}

		// L'utente richiede di aprire le immagini singolarmente. accontentiamolo (sigh)
		openFileSingularly(pathFile);
	}

	private String chooseInitialFile() {
		OpenDialog od = new OpenDialog("Selezionare un'immagine");

		String dir = od.getDirectory();
		String name = od.getFileName();
		return (dir + name);
	}

	private ImporterOptions generateIRSTOptions(String pathFile) throws IOException {
		ImporterOptions options = new ImporterOptions();
		options.setColorMode(ImporterOptions.COLOR_MODE_DEFAULT);
		options.setId(pathFile);
		// Per aprire files in maniera "ridotta": imps = BF.openThumbImagePlus(options);
		return options;
	}

	// TODO: Controllare se sia necessario passare pathFile! forse basta passargli l'imp
	private void openFileSingularly(String pathFile) {
		ImageProcessorReader imageProcessorReader = new ImageProcessorReader(
				new ChannelSeparator(LociPrefs.makeImageReader()));
		try {
			imageProcessorReader.setId(pathFile);
			int imageCount = imageProcessorReader.getImageCount();


			for (int i = 0; i < imageCount ; i++) {
				ImageProcessor imageProcessor = imageProcessorReader.openProcessors(i)[0];
				ImagePlus plus = new ImagePlus("c" + i, imageProcessor);
				plus.show();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void preview() {

	}

	@Override
	public void cancel() {

	}
}

