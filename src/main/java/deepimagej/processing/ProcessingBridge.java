/*
 * DeepImageJ
 * 
 * https://deepimagej.github.io/deepimagej/
 * 
 * Reference: DeepImageJ: A user-friendly environment to run deep learning models in ImageJ
 * E. Gomez-de-Mariscal, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
 * Submitted 2021.
 * Bioengineering and Aerospace Engineering Department, Universidad Carlos III de Madrid, Spain
 * Biomedical Imaging Group, Ecole polytechnique federale de Lausanne (EPFL), Switzerland
 * Science for Life Laboratory, School of Engineering Sciences in Chemistry, Biotechnology and Health, KTH - Royal Institute of Technology, Sweden
 * 
 * Authors: Carlos Garcia-Lopez-de-Haro and Estibaliz Gomez-de-Mariscal
 *
 */

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2019-2021, DeepImageJ
 * All rights reserved.
 *	
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
 *	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package deepimagej.processing;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import deepimagej.Parameters;
import deepimagej.exceptions.JavaProcessingError;
import deepimagej.exceptions.MacrosError;
import deepimagej.tools.DijTensor;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextWindow;

public class ProcessingBridge {
	
	// TODO decide whether to allow or not more than 1 image input to the model
	public static HashMap<String, Object> runPreprocessing(ImagePlus im, Parameters params) throws MacrosError, JavaProcessingError, NoSuchMethodException, SecurityException,
																					IllegalAccessException, IllegalArgumentException, InvocationTargetException,
																					ClassNotFoundException, InstantiationException, IOException {
		HashMap<String, Object> map = new HashMap<String, Object>();
		params.javaPreprocessingClass = new ArrayList<String>();
		int inputImageInd = DijTensor.getImageTensorInd(params.inputList);
		// Assume that the image selected will result in the input image to the model
		// Assumes 'im' will be the input to the model
		if (params.firstPreprocessing != null && (params.firstPreprocessing.contains(".txt") || params.firstPreprocessing.contains(".ijm"))) {
			im = runProcessingMacro(im, params.firstPreprocessing, params.developer);
			map = manageInputs(map, false, params, im);
		} else if (params.firstPreprocessing != null && (params.firstPreprocessing.contains(".jar") || params.firstPreprocessing.contains(".class") || new File(params.firstPreprocessing).isDirectory())) {
			map.put(params.inputList.get(inputImageInd).name, im);
			map = runPreprocessingJava(map, params.firstPreprocessing, params.attachments, params);
		}
		

		if (params.secondPreprocessing != null && (params.secondPreprocessing.contains(".txt") || params.secondPreprocessing.contains(".ijm"))) {
			im = runProcessingMacro(im, params.secondPreprocessing, params.developer);
			map = manageInputs(map, true,  params, im);
		} else if (params.secondPreprocessing != null && (params.secondPreprocessing.contains(".jar") || params.secondPreprocessing.contains(".class") || new File(params.secondPreprocessing).isDirectory())) {
			if (map.keySet().size() == 0)
				map.put(params.inputList.get(inputImageInd).name, im);
			map = runPreprocessingJava(map, params.secondPreprocessing, params.attachments, params);
		} else if (params.secondPreprocessing == null && (params.firstPreprocessing == null || params.firstPreprocessing.contains(".txt") || params.firstPreprocessing.contains(".ijm"))) {
			map = manageInputs(map, true, params);
		} else if (params.secondPreprocessing == null && (params.firstPreprocessing.contains(".jar") || params.secondPreprocessing.contains(".class") || new File(params.firstPreprocessing).isDirectory())) {
			//TODO check if an input is missing. If it is missing try to recover it from the workspace.
		}
		return map;
	}
	
	private static HashMap<String, Object> manageInputs(HashMap<String, Object> map, boolean lastStep, Parameters params){
		 map = manageInputs(map, lastStep, params, null);
		 return map;
	}
	
	/*
	 * Updates the map containing the inputs. In principle, this is used only if there has not
	 * beeen Java processing before (Java processing should already output a map). 
	 * This method assumes that each model input has an ImageJ object associated. Except for
	 * the main image, where if it is not named correctly, assumes it is the originally referenced
	 * image (line 62).
	 */
	private static HashMap<String, Object> manageInputs(HashMap<String, Object> map, boolean lastStep, Parameters params, ImagePlus im) {
		for (DijTensor tensor : params.inputList) {
			if (tensor.tensorType == "image") {
				ImagePlus inputImage = WindowManager.getImage(tensor.name);
				if (inputImage != null) {
					map.put(tensor.name, inputImage);
		        } else if (map.get(tensor.name) == null && im != null) {
					map.put(tensor.name, im);
		        } else if (map.get(tensor.name) == null && im == null && lastStep) {
		        	im = WindowManager.getCurrentImage();
		        	if (im == null) {
			        	im = WindowManager.getTempCurrentImage();
		        	}
					map.put(tensor.name, im);
		        }
			} else if (tensor.tensorType == "parameter") {
				Frame f = WindowManager.getFrame(tensor.name);
		        if (f!=null && (f instanceof TextWindow)) {
		        	 ResultsTable inputTable = ((TextWindow)f).getResultsTable();
					map.put(tensor.name, inputTable);
		        } else if (map.get(tensor.name) == null && lastStep){
		        	IJ.error("There is no ResultsTable named: " + tensor.name + ".\n" +
		        			"There should be as it is one of the inputs required\n"
		        			+ "by the model.");
		        	return null;
		        }
			}
		}
		return map;
	}

	/****************************************+
	 * Method to run a pre-processing routine in Java on the model inputs
	 * @param map: hashmap containing all the images and tables opened in ImageJ with the keys
	 * being the title of the window
	 * @param processingPath: path to the java file that specifies the processing
	 * @param config: path to the config file. The config file is considered to be the other
	 * pre-tprocessing file. HOwever it only will be used if it is not null and ends with .txt or .ijm
	 * @param params: model parameters
	 * @return map: hashmap containing the results of the processing routine
	 * @throws JavaProcessingError 
	 * @throws IOException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	private static HashMap<String, Object> runPreprocessingJava(HashMap<String, Object> map, String processingPath, ArrayList<String> config, Parameters params) throws JavaProcessingError, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InstantiationException, IOException {
		boolean preprocessing = true;
		ExternalClassManager processingRunner = new ExternalClassManager (processingPath, preprocessing, params);
		map = processingRunner.javaPreprocess(map, config);
		return map;
	}

	private static ImagePlus runProcessingMacro(ImagePlus img, String macroPath, boolean developer) throws MacrosError {
		WindowManager.setTempCurrentImage(img);
		String aborted = "";
		try {
			aborted = IJ.runMacroFile(macroPath);
		} catch (Exception ex) {
			aborted = "[aborted]";
		}
		
		if (aborted == "[aborted]") {
			throw new MacrosError();
		}
		
		ImagePlus result = WindowManager.getCurrentImage();
		// If the macro opens the image, close it
		if (result.isVisible() && !developer)
			result.getWindow().dispose();
		return result;
	}
	
	/******************************************************************************************************************
	 * Method to run the wanted post-processing wanted on the images or tables 
	 * produced by the deep learning model
	 * @param params: parameters of the moel. It contains the path to the post-processing files
	 * @param map: hashmap containing all the outputs given by the model. The keys are the names 
	 * 	given by the model to each of the outputs. And the values are either ImagePlus or ResultsTable.
	 * @return map: map containing all the paths to the processing files
	 * @throws MacrosError is thrown if the Macro file does not work
	 * @throws JavaProcessingError 
	 * @throws IOException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	public static HashMap<String, Object> runPostprocessing(Parameters params, HashMap<String, Object> map) throws MacrosError, JavaProcessingError,
														NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
														InvocationTargetException, ClassNotFoundException, InstantiationException,
														IOException {

		params.javaPostprocessingClass = new ArrayList<String>();
		
		if (params.firstPostprocessing != null && (params.firstPostprocessing.contains(".txt") || params.firstPostprocessing.contains(".ijm"))) {
			runPostprocessingMacro(params.firstPostprocessing);
			map = manageOutputs(map);
		} else if (params.firstPostprocessing != null && (params.firstPostprocessing.contains(".jar") || params.firstPostprocessing.contains(".class") || new File(params.firstPostprocessing).isDirectory())) {
			map = runPostprocessingJava(map, params.firstPostprocessing, params.attachments, params);
		}
		

		if (params.secondPostprocessing != null && (params.secondPostprocessing.contains(".txt") || params.secondPostprocessing.contains(".ijm"))) {
			runPostprocessingMacro(params.secondPostprocessing);
		} else if (params.secondPostprocessing != null && (params.secondPostprocessing.contains(".jar") || params.secondPostprocessing.contains(".class") || new File(params.secondPostprocessing).isDirectory())) {
			map = runPostprocessingJava(map, params.secondPostprocessing, params.attachments, params);
		}
		return map;
	}
	
	/****************************************+
	 * Method to run a post-processing routine in Java on the images and table open in
	 * ImageJ
	 * @param map: hashmap containing all the images and tables opened in ImageJ with the keys
	 * being the title of the window
	 * @param processingPath: path to the java file that specifies the processing
	 * @param config: path to the config file. The config file is considered to be the other
	 * postprocessing file. HOwever it only will be used if it is not null and ends with .txt or .ijm
	 * @param params: model parameters
	 * @return map: hashmap containing the results of the processing routine
	 * @throws JavaProcessingError 
	 * @throws IOException 
	 * @throws InstantiationException 
	 * @throws ClassNotFoundException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 */
	private static HashMap<String, Object> runPostprocessingJava(HashMap<String, Object> map, String processingPath,
																 ArrayList<String> config, Parameters params) throws 
																 JavaProcessingError, NoSuchMethodException,
																 SecurityException, IllegalAccessException,
																 IllegalArgumentException, InvocationTargetException,
																 ClassNotFoundException, InstantiationException, IOException {
		boolean preprocessing = false;
		ExternalClassManager processingRunner = new ExternalClassManager (processingPath, preprocessing, params);
		map = processingRunner.javaPostprocess(map, config);
		return map;
	}

	/***************************
	 * Method to run a macro processing routine over the outputs of the model. 
	 * @param macroPath: path to the macro file
	 * @return: last image processed by the file
	 * @throws MacrosError: thrown if the macro contains errors
	 */
	private static void runPostprocessingMacro(String macroPath) throws MacrosError {

		String aborted = IJ.runMacroFile(macroPath);
		if (aborted == "[aborted]") {
			throw new MacrosError();
		}
	}
	
	/**************************
	 * Method that puts all the images and results tables with their names
	 * in a hashmap.
	 * @return map: hashmap containing all the images and results tables.
	 */
	private static HashMap<String, Object> manageOutputs(HashMap<String, Object> map) {
		Frame[] nonImageWindows = WindowManager.getNonImageWindows();
		String[] imageTitles = WindowManager.getImageTitles();
		for (String title : imageTitles) {
			map.put(title, WindowManager.getImage(title));
		}
		for (Frame f : nonImageWindows) {
	        if (f!=null && (f instanceof TextWindow)) {
	        	String tableTitle = f.getTitle();
	        	ResultsTable table = ((TextWindow)f).getResultsTable();
				map.put(tableTitle, table);
	        }
		}
		return map;
	}

}
