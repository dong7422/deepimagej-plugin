/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 *
 * Conditions of use: You are free to use this software for research or educational purposes. 
 * In addition, we strongly encourage you to include adequate citations and acknowledgments 
 * whenever you present or publish results that are based on it.
 * 
 * Reference: DeepImageJ: A user-friendly plugin to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, L. Donati, M. Unser, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2019.
 *
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 *
 * Corresponding authors: mamunozb@ing.uc3m.es, daniel.sage@epfl.ch
 *
 */

/*
 * Copyright 2019. Universidad Carlos III, Madrid, Spain and EPFL, Lausanne, Switzerland.
 * 
 * This file is part of DeepImageJ.
 * 
 * DeepImageJ is an open source software (OSS): you can redistribute it and/or modify it under 
 * the terms of the BSD 2-Clause License.
 * 
 * DeepImageJ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 * You should have received a copy of the BSD 2-Clause License along with DeepImageJ. 
 * If not, see <https://opensource.org/licenses/bsd-license.php>.
 */

package deepimagej.stamp;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.protobuf.InvalidProtocolBufferException;

import deepimagej.BuildDialog;
import deepimagej.Constants;
import deepimagej.DeepImageJ;
import deepimagej.Parameters;
import deepimagej.RunnerTf;
import deepimagej.RunnerProgress;
import deepimagej.RunnerPt;
import deepimagej.TensorFlowModel;
import deepimagej.components.GridPanel;
import deepimagej.components.HTMLPane;
import deepimagej.exceptions.IncorrectChannelsSlicesNumber;
import deepimagej.exceptions.JavaProcessingError;
import deepimagej.exceptions.MacrosError;
import deepimagej.processing.ProcessingBridge;
import deepimagej.tools.ArrayOperations;
import deepimagej.tools.DijTensor;
import deepimagej.tools.Log;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

public class TestStamp extends AbstractStamp implements Runnable, ActionListener {

	private HTMLPane				pnTest;
	private JButton					bnTest	= new JButton("Run a test");
	private List<JComboBox<String>>	cmbList	= new ArrayList<JComboBox<String>>();
	private List<JButton>			btnList	= new ArrayList<JButton>();
	private JPanel					inputsPn = new JPanel();
	private HashMap<String, Object> inputsMap;
	
	private List<DijTensor>		imageTensors;

	public TestStamp(BuildDialog parent) {
		super(parent);
		buildPanel();
	}

	public void buildPanel() {
		SwingUtilities.invokeLater(() -> {

		pnTest = new HTMLPane(Constants.width, 100);
		JScrollPane pnTestScroller = new JScrollPane(pnTest);
		//pnTestScroller.setPreferredSize(new Dimension(Constants.width, pnTest.getPreferredSize().height));
		HTMLPane pane = new HTMLPane(Constants.width, 100);
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.append("h2", "Run a test on an image");
		pane.append("p", "Select on input image and click on 'Run a test ");
		GridPanel pn1 = new GridPanel(true);
		JComboBox<String> cmb = new JComboBox<String>();
		cmbList.add(cmb);
		JButton btn = retrieveJComboBoxArrow(cmb);
		btnList.add(btn);
		inputsPn.add(new JLabel("Image"));
		inputsPn.add(cmb);
		pn1.place(1, 0, inputsPn);
		pn1.place(2, 0, bnTest);

		JPanel pnt = new JPanel();
		pnt.setLayout(new BoxLayout(pnt, BoxLayout.PAGE_AXIS));
		pnt.add(pane.getPane());
		pnt.add(pn1);

		JPanel pn = new JPanel(new BorderLayout());
		pn.add(pnt, BorderLayout.NORTH);
		//pn.add(pnTest, BorderLayout.CENTER);
		pn.add(pnTestScroller, BorderLayout.CENTER);
		
		pnTest.setEnabled(true);

		panel.add(pn);
		bnTest.addActionListener(this);
		});
	}

	@Override
	public void init() {
		Parameters params = parent.getDeepPlugin().params;
		inputsPn.removeAll();
		imageTensors = DijTensor.getImageTensors(params.inputList);
		inputsPn.setLayout(new GridLayout(2, imageTensors.size()));
		cmbList = new ArrayList<JComboBox<String>>();
		btnList = new ArrayList<JButton>();
		JComboBox<String> cmb = new JComboBox<String>();
		cmb = new JComboBox<String>();
		String[] titlesList = WindowManager.getImageTitles();
		int c = 0;
		for (DijTensor tensor : imageTensors) {
			cmb = new JComboBox<String>();
			if (titlesList.length != 0) {
				for (String title : titlesList)
					cmb.addItem(title);
				inputsPn.add(new JLabel(tensor.name));
				cmbList.add(cmb);
				inputsPn.add(cmb);
				bnTest.setEnabled(parent.getDeepPlugin() != null);
			} else {
				bnTest.setEnabled(false);
				params.testImageBackup = null;
				cmb.addItem("No image");
				cmbList.add(cmb);
				inputsPn.add(new JLabel("Select image"));
				inputsPn.add(cmb);
			}
			btnList.add(retrieveJComboBoxArrow(cmb));
			btnList.get(c).addActionListener(this);
			cmbList.get(c ++).addActionListener(this);
		}
	}

	@Override
	public boolean finish() {
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnTest) {
			// Check if all the input images are associated to an image
			// opened in ImageJ
			String[] titlesList = WindowManager.getImageTitles();
			for (int j = 0; j < cmbList.size(); j ++) {
				String selectedOption = (String) cmbList.get(j).getSelectedItem();
				if (Arrays.asList(titlesList).contains(selectedOption))
					continue;
				 else {
					 IJ.error("Select images open in ImageJ");
					return;
				}	
			}
			// If all the images selected are opened in ImageJ un test
			test();
		}
		
		for (int i = 0; i < btnList.size(); i ++) {
			if (e.getSource() == btnList.get(i)) {
				String[] titlesList = WindowManager.getImageTitles();

				cmbList.get(0).removeActionListener(this);
				cmbList.get(0).removeAllItems();
				// Update the list of options provided by each of 
				// the images
				for (int j = 0; j < cmbList.size(); j ++) {
					//cmbList.get(j).removeAllItems();
					if (titlesList.length != 0) {
						for (String title : titlesList)
							cmbList.get(j).addItem(title);
						bnTest.setEnabled(true);
					} else {
						cmbList.get(j).addItem("No image");
						bnTest.setEnabled(false);
					}	
				}
				cmbList.get(0).addActionListener(this);
				
				break;
			}
		}
		
		for (int i = 0; i < cmbList.size(); i ++) {
			if (e.getSource() == cmbList.get(i)) {
				// If all the selected items in every cmbBox correspond
				// to an existing image, set the button enabled, if not, 
				// not enabled
				cmbList.get(0).removeActionListener(this);
				String[] titlesList = WindowManager.getImageTitles();
				for (int j = 0; j < cmbList.size(); j ++) {
					String selectedOption = (String) cmbList.get(j).getSelectedItem();
					if (Arrays.asList(titlesList).contains(selectedOption)) {
						continue;
					} else {
						 bnTest.setEnabled(false);
						return;
					}	
				}
				bnTest.setEnabled(true);
				cmbList.get(0).addActionListener(this);
				
				break;
			}
		}
	}

	public void test() {
		Parameters params = parent.getDeepPlugin().params;
		inputsMap = new HashMap<String, Object>();

		File file = new File(params.path2Model);
		if (!file.exists()) {
			IJ.error("No selected model.");
			return;
		}
		String dirname = file.getName();
		bnTest.setEnabled(false);
		pnTest.append("h2", "Test " + dirname);
		
		String[] images = new String[imageTensors.size()];
		for (int i = 0; i < images.length; i++)
			images[i] = (String)cmbList.get(i).getSelectedItem();
		if (images.length == 1)
			params.testImage = WindowManager.getImage(images[0]);
		String imagesNames = Arrays.toString(images);

		for (String im : images) {
			if (WindowManager.getImage(im) == null) {
				pnTest.append("p", im + " does not correspond to an open image");
				IJ.error("No selected test image.");
				return;
			}
			params.testImageBackup = WindowManager.getImage(im).duplicate();
			params.testImageBackup.setTitle("DUP_" + im);
		}
		
		pnTest.append("Selected input images " + imagesNames);
	
		try {
			// Create a HashMap of the inputs to feed it to the Runner class
			inputsMap = ProcessingBridge.runPreprocessing(params.testImage, params);
			// Check if the images have the adequate channels and slices
			for (DijTensor tensor : imageTensors) {
				// TODO keep or remove
				if (((ImagePlus) inputsMap.get(tensor.name)).getType() == 4){
					IJ.run((ImagePlus) inputsMap.get(tensor.name), "Make Composite", "");
					inputsMap.put(tensor.name, WindowManager.getCurrentImage());
				}
				// TODO fix this for the case where slices or channels are not fixed
				int channels = TensorFlowModel.nChannelsOrSlices(tensor, "channels");
				int slices = TensorFlowModel.nChannelsOrSlices(tensor, "slices");
				int imageChannels = ((ImagePlus) inputsMap.get(tensor.name)).getNChannels();
				int imageSlices = ((ImagePlus) inputsMap.get(tensor.name)).getNSlices();
				if (channels != imageChannels) {
					throw new IncorrectChannelsSlicesNumber(channels, imageChannels, "channels");
				}
				if (slices != imageSlices) {
					throw new IncorrectChannelsSlicesNumber(slices, imageSlices, "slices");
				}
			}

			if (inputsMap != null){
				Thread thread = new Thread(this);
				thread.setPriority(Thread.MIN_PRIORITY);
				thread.start();
			}
		}
		catch (MacrosError e1) {
			pnTest.append("p", "Error in the  preprocessing Macro's code");
			IJ.error("Failed preprocessing");
		} // TODO remove. It seems that it does no make sense
		catch (IllegalArgumentException e1) {
			IJ.error("The model failed to execute because " + "at some point \nthe size of the activations map was incorrect");
			e1.printStackTrace();
		} // TODO remove. It seems that it does no make sense
		catch (UnsupportedOperationException e) {
			pnTest.append("p", "The model could not be executed properly. Try with another parameters.\n");
		}
		catch (IncorrectChannelsSlicesNumber e) {
			String type = e.getExceptionType();
			pnTest.append("p", "The number of " + type + " of the input image is incorrect.");
			IJ.error("The number of " + type + " of the input image is incorrect.");
		} catch (JavaProcessingError e) {
			e.printStackTrace();
			pnTest.append("p", "Error in the preprocessing external Java code");
			IJ.error("Failed preprocessing");
		}
	}

	public void run() {
		DeepImageJ dp = parent.getDeepPlugin();

		Log log = new Log();
		RunnerProgress rp = new RunnerProgress(dp, false);
		HashMap<String, Object> output = null;
		if (dp.params.framework.equals("Tensorflow")) {
			RunnerTf runner = new RunnerTf(dp, rp, inputsMap, log);
			rp.setRunner(runner);
			// TODO decide what to store at the end of the execution
			output = runner.call();
		} else {
			RunnerPt runner = new RunnerPt(dp, rp, inputsMap, log);
			rp.setRunner(runner);
			// TODO decide what to store at the end of the execution
			output = runner.call();
		}
		// Flag to apply post processing if needed
		if (output == null) {
			pnTest.append("p", "Test run failed");
			IJ.error("The execution of the model failed.");
			return;
		}
		
		try {
			output = ProcessingBridge.runPostprocessing(dp.params, output);
		} catch (MacrosError e) {
			e.printStackTrace();
			pnTest.append("p", "Error in the  postprocessing Macro's code");
			IJ.error("Failed postprocessing");
		} catch (JavaProcessingError e) {
			e.printStackTrace();
			pnTest.append("p", "Error in the postprocessing external Java code");
			IJ.error("Failed postprocessing");
		}
		
		// Print the outputs of the postprocessing
		// Retrieve the opened windows and compare them to what the model has outputed
		// Display only what has not already been displayed

		String[] finalFrames = WindowManager.getNonImageTitles();
		String[] finalImages = WindowManager.getImageTitles();
		ArrayOperations.displayMissingOutputs(finalImages, finalFrames, output);
		
		parent.endsTest();
		bnTest.setEnabled(true);
		//dp.params.testResultImage[0].getProcessor().resetMinAndMax();
		//dp.params.testResultImage[0].show();
		pnTest.append("p", "Peak memory:" + dp.params.memoryPeak);
		pnTest.append("p", "Runtime:" + dp.params.runtime);
	}
	
	public JButton retrieveJComboBoxArrow(Container container) {
		if (container instanceof JButton) {
	         return (JButton) container;
		} else {
			Component[] components = container.getComponents();
			for (Component component : components) {
				if (component instanceof Container) {
					return retrieveJComboBoxArrow((Container)component);
	            }
			}
	    }
		return null;
	}
	
}
