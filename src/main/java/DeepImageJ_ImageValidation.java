/*
 * DeepImageJ
, C. Garcia-Lopez-de-Haro, W. Ouyang, L. Donati, M. Unser, E. Lundberg, A. Munoz-Barrutia, D. Sage. 
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

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Vector;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import deepimagej.validation.AbstractLoss;
import deepimagej.validation.Bce;
import deepimagej.validation.CategoricalHinge;
import deepimagej.validation.Composed;
import deepimagej.validation.DiceLoss;
import deepimagej.validation.Hinge;
import deepimagej.validation.Jaccard;
import deepimagej.validation.KLD;
import deepimagej.validation.LAP;
import deepimagej.validation.Log_Cosh;
import deepimagej.validation.MAE;
import deepimagej.validation.MAPE;
import deepimagej.validation.MSLE;
import deepimagej.validation.RMSE;
import deepimagej.validation.RegressSNR;
import deepimagej.validation.NormL1;
import deepimagej.validation.NormL2;
import deepimagej.validation.PSNR;
import deepimagej.validation.Poisson;
import deepimagej.validation.SNR;
import deepimagej.validation.SSIM;
import deepimagej.validation.Constants;
import deepimagej.validation.Settings;
import deepimagej.validation.Square_Hinge;



public class DeepImageJ_ImageValidation implements ActionListener,PlugIn, ItemListener{

	private static String title1 = "";
	private static String title2 = "";

	
	private TextArea info;
	
	private GenericDialog gd ;
	Settings set = new Settings();
	Panel settings = new Panel();
	Button button = new Button("Advanced");
	private Constants setting = new Constants();
	public int columns_function = 4 , rows_function = 6;
	public int columns_text = 40 , rows_text = 3;
	
	
	
	public static void main(String arg[]) {
		new ImageJ();
		ImagePlus imp = IJ.openImage("C:\\Users\\Carlos(tfg)\\3D Objects\\source\\models\\mulux\\prueba.tif");
		if (imp != null)
			imp.show();
		ImagePlus imp2 = IJ.openImage("C:\\Users\\Carlos(tfg)\\3D Objects\\source\\models\\mulux\\prueba.tif");
		if (imp2 != null)
			imp2.show();
		new DeepImageJ_ImageValidation().run("");
	}

	public void run(String arg) {
		
		// import loss functions an array list
		ArrayList<AbstractLoss> functions = new ArrayList<AbstractLoss>();
		functions.add(new NormL1());
		functions.add(new NormL2());
		functions.add(new Bce());
		functions.add(new RMSE());
		functions.add(new MAE());
		functions.add(new SNR());
		functions.add(new PSNR());
		functions.add(new DiceLoss());
		functions.add(new Jaccard());
		functions.add(new SSIM());
		functions.add(new RegressSNR());
		functions.add(new LAP());
		// TODO decide what to do with function composition functions.add(new Composed());
		functions.add(new KLD());
		functions.add(new MAPE());
		functions.add(new MSLE());
		functions.add(new CategoricalHinge());
		functions.add(new Hinge());
		functions.add(new Log_Cosh());
		functions.add(new Poisson());
		functions.add(new Square_Hinge());
		
		
		
		
		// create table of text with infos about images loaded
		info = new TextArea("Information ", rows_text, columns_text, TextArea.SCROLLBARS_VERTICAL_ONLY);
		info.setEditable(false);
		
		
		//get list of images open in ImageJ
		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.noImage();
			return;
		}
		
		//get infos about the image put initially in the scroll bar 
		ImagePlus img_initial = WindowManager.getImage(wList[0]);
		int nx1= img_initial.getWidth();
		int ny1= img_initial.getHeight();
		int BitDepth1 = img_initial.getBitDepth();
		int typ1= img_initial.getType();
		int NSlices1 = img_initial.getNSlices();
		
		//corresponding text to the categories of images
		String[] cat ={"GRAY8","GRAY16","GRAY32","COLOR_256","COLOR_RGB"};
		
		//set text in the edit text
		info.setText( img_initial.getTitle() + " "  + "Size: " + " [" + nx1 + " " + ny1 + "]" + " , " + "BitDepth: " + BitDepth1 + " , " + "NSlices: " + NSlices1 + " , " + "Cat : "+ cat[typ1] + "\n");
		info.append( img_initial.getTitle() + " "  + "Size: " + " [" + nx1 + " " + ny1 + "]" + " , " + "BitDepth: " + BitDepth1 + " , " + "NSlices: " + NSlices1 + " , " + "Cat : "+ cat[typ1] + "\n");
		
		Panel panel = new Panel(new BorderLayout());
		panel.add(info, BorderLayout.CENTER);

		
		//Create a list with titles of all images
		String[] titles = new String[wList.length];
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp != null)
				titles[i] = imp.getTitle();
			else
				titles[i] = "";
		}

		//create the box of dialog
		gd = new GenericDialog("Loss Functions");
		
		//Create scroll bar to choose images to be used
		String defaultItem;
		if (title1.equals(""))
			defaultItem = titles[0];
		else
			defaultItem = title1;
		gd.addChoice("Reference image:", titles, defaultItem);
		if (title2.equals(""))
			defaultItem = titles[0];
		else
			defaultItem = title2;
		gd.addChoice("Test image:", titles, defaultItem);
		
		//add the box of text to the dialog
		gd.addPanel(panel);
		
		
		//get the names of the different loss functions and create a matrix of checkboxes with those one
		String name[] = new String[functions.size()];
		boolean content[] = new boolean[functions.size()];
		
		int j=0;
		for(AbstractLoss function : functions) {
			name[j] = function.getName();
			content[j] = function.getName().contentEquals("NormL2");
			j++;
		}
		
		gd.addCheckboxGroup(rows_function , columns_function , name, content);
		
		//create a button "settings" to set the parameters of different loss functions
		button.addActionListener(this);
		settings.add(button);
		gd.addPanel(settings);
		//help button
		gd.addHelp("https://deepimagej.github.io/deepimagej/download.html");

		for (Component c : gd.getComponents()) {
			if (c instanceof Choice) {
				Choice choice = (Choice) c;
				choice.addItemListener(this);
			}
		}
		
		//put a numeric fiel to know the number of decimals the user wants
		gd.addNumericField("Decimal places (0-9):", 3, 0);
		
		//display the dialog box 
		gd.showDialog();
		
		// Now we will get the infos of the dialog box 
		
		
		int decimals = (int)gd.getNextNumber();
		
		//set true to function selected and false to others
		for(AbstractLoss function : functions)
			function.setSelected(gd.getNextBoolean());
		
		//cancel the dialog box
		if (gd.wasCanceled())
			return;
		
		//get the images selected in the scrollbar
		int index1 = gd.getNextChoiceIndex();
		title1 = titles[index1];
		int index2 = gd.getNextChoiceIndex();
		title2 = titles[index2];
		info.setText(title2);
		ImagePlus img1 = WindowManager.getImage(wList[index1]);
		ImagePlus img2 = WindowManager.getImage(wList[index2]);
		
		// TODO in the future allow more than 2d images
		if (img1.getNChannels() > 1 || img1.getNFrames() > 1 || img1.getNSlices() > 1) {
			IJ.error("DeepImageJ Validate only supports 2D images with 1 channel, 1 slice and 1 time frame\n"
					+ "Those conditions are not fulfilled by: " + img1.getTitle());
			return;
		}
		if (img2.getNChannels() > 1 || img2.getNFrames() > 1 || img2.getNSlices() > 1) {
			IJ.error("DeepImageJ Validate only supports 2D images with 1 channel, 1 slice and 1 time frame\n"
					+ "Those conditions are not fulfilled by: " + img2.getTitle());
			return;
		}
		

		
		ResultsTable table = new ResultsTable();
		
		int stack1=1;
		int stack2=1;
		
		Boolean increment_both=false;
		Boolean increment_first=false;
		Boolean increment_sec=false;
		Boolean segmented=false;
		int nzr = img1.getStack().getSize();
		int nzt = img2.getStack().getSize();
		int nb_function_selected=0;
		int nb_function_segmented=0;
		
		int place_jacc = 0; //find the place of the segmented
		
		int nxr1 = img1.getWidth();
		int nyr1 = img1.getHeight();
		int nxr2 = img1.getWidth();
		int nyr2 = img1.getHeight();
		
		if(nxr1 != nxr2 || nyr1 != nyr2 )
			IJ.error("Images do not have the same dimensions");
		
		
		place_jacc =(int)img1.getStack().getProcessor(1).getStatistics().max + 1;
		
		//Verify some constraints of the loss functions and display an error if there is one
		for(AbstractLoss function : functions)
			if (function.getSelected()) {
				String valid = function.check(img1, img2, setting);
				if(!valid.equals("Valid")) {
					IJ.error(valid);
					function.setSelected(false);
					//return;
				}
			}
		
		for(AbstractLoss function : functions)
			if (function.getSelected()) {
				if(function.getSegmented() == true) {
					segmented=true;
					nb_function_segmented++;
				}
				else {
					nb_function_selected++;
				}
			}
		
		//create a table which will store results
		String results[][]= new String[nb_function_selected][Math.max(nzr, nzt) + 1];
		String results_segmented[][]= new String[nb_function_segmented][place_jacc*Math.max(nzr, nzt) + 1];
		ArrayList<String> funcname = new ArrayList<String>();
		ArrayList<String> funcname_segmented = new ArrayList<String>();
		
		// check for the number of stacks if the reference and test havve multiple stacks 
		if(nzr==nzt) {
			increment_both=true;
		}
		else if(nzr>nzt) {
			if(nzt==1) {
				increment_first=true;
			}
			else {
				IJ.error("Wrong number of stacks");
				return;
			}
		}
		else if(nzr<nzt) {
			if(nzr==1) {
				increment_sec=true;
			}
			else {
				IJ.error("Wrong number of stacks");
				return;
			}
		}
		
		int nfunc=0; //function counter 
		int nfunc_segmented=0;
		int nloss=0;// image comparison counter
		
		
		
		ArrayList<Double> loss1_comp = null, loss2_comp = null;
		//call the classes of loss function to get the results
		for(AbstractLoss function : functions) {
			if (function.getSelected()) { 
				ArrayList<Double> losses= null;
				if(function.getName()=="Composed Function") {
					losses= function.compose(loss1_comp, setting.w1_composed, loss2_comp, setting.w2_composed);
				}
				else {
					losses= function.run(img1, img2,setting);
					if(function.getName()== setting.title1) {
						loss1_comp=losses;
					}
					else if(function.getName()== setting.title2) {
						loss2_comp=losses;
					}
				}
				
				//store in a special list for segmented
				if(function.getSegmented() == true) {
					funcname_segmented.add(function.getName());
					nloss=0;
					if (losses == null)
						return;
					for(Double loss : losses ) {
						
						results_segmented[nfunc_segmented][nloss]=String.format("%.0"+Integer.toString(decimals)+"f",loss);
						nloss++;
					}
					nfunc_segmented++;
					
				}
				else {
					funcname.add(function.getName());
					nloss=0;
					
					for(Double loss : losses ) {
						
						results[nfunc][nloss]=String.format("%.0"+Integer.toString(decimals)+"f",loss);
						nloss++;
					}
					
					nfunc++;
				}
				
				
			}
			else {
				
				if(function.getName()== setting.title1) {
					loss1_comp = function.run(img1, img2,setting);
				}
				else if(function.getName()== setting.title2) {
					loss2_comp = function.run(img1, img2,setting);
				}
			}
		}
		
		int no_im_jacc=0;
		//Create the table of results
		for (int l = 0; l < Math.max(nzr, nzt) ; l++) {
			if(segmented) {
				// Firs add the global measure.
				table.incrementCounter();
				table.addValue("ref", img1.getTitle());
				table.addValue("test", img2.getTitle());
				table.addValue("N Ref", Integer.toString(stack1)+"(global)");
				table.addValue("N Test", Integer.toString(stack2)+"(global)");
				for (int i = 0; i < funcname_segmented.size(); i++) {
					// The global value is saved at the end
					if (results_segmented[i] != null)
						table.addValue(funcname_segmented.get(i), results_segmented[i][results_segmented[i].length - 1]);
					else
						table.addValue(funcname_segmented.get(i), "ERROR");
				}
				for (int i = 0; i < funcname.size(); i++) {
					table.addValue(funcname.get(i), results[i][l]);
				}
				for(int m = 0; m < place_jacc; m++) {
					// Check which pixels exist on the image
					boolean existsPixel = false;
					boolean[] valueExists = new boolean[funcname_segmented.size()];
					for (int i = 0; i < funcname_segmented.size(); i++) {
						if (results_segmented[i] == null)
							continue;
						double val = Double.parseDouble(results_segmented[i][no_im_jacc+m]);
						valueExists[i] = val != -1;
						if (valueExists[i])
							existsPixel = true;
					}
					// If the pixel does not exist on the image, do not represent its value
					if (!existsPixel)
						continue;
					
					table.incrementCounter();
					table.addValue("ref", img1.getTitle());
					table.addValue("test", img2.getTitle());
					table.addValue("N Ref", Integer.toString(stack1)+"("+Integer.toString(m)+")");
					table.addValue("N Test", Integer.toString(stack2)+"("+Integer.toString(m)+")");
					for (int i = 0; i < funcname_segmented.size(); i++) {
						if (valueExists[i])
							table.addValue(funcname_segmented.get(i), results_segmented[i][no_im_jacc+m]);
						else
							table.addValue(funcname_segmented.get(i), "NA");							
					}
					for (int i = 0; i < funcname.size(); i++) {
						table.addValue(funcname.get(i), "NA");
					}
					
				}
				no_im_jacc+=place_jacc;
				
				if (increment_both) {
					stack1++;
					stack2++;
				}
				else if(increment_first) {
					stack1++;
				}
				else if(increment_sec) {
					stack2++;
				}
			}
			else {
				
				table.incrementCounter();
				table.addValue("ref", img1.getTitle());
				table.addValue("test", img2.getTitle());
				table.addValue("N Ref", stack1);
				table.addValue("N Test", stack2);
				for (int i = 0; i < funcname.size(); i++) {
					table.addValue(funcname.get(i), results[i][l]);
				}
				if (increment_both) {
					stack1++;
					stack2++;
				}
				else if(increment_first) {
					stack1++;
				}
				else if(increment_sec) {
					stack2++;
				}
				
			}			
		}
		// display table of results
		table.show("Loss");
		
		
		
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		
		//get the change of choice in the scroll bar 
		Vector choices = gd.getChoices();
		Choice choice_one = (Choice)choices.elementAt(0);
		Choice choice_two = (Choice)choices.elementAt(1);
		String item1 = choice_one.getSelectedItem();
		String item2 = choice_two.getSelectedItem();
		
		//get infos about the 2 images selected
		ImagePlus img1 = WindowManager.getImage(item1);
		ImagePlus img2 = WindowManager.getImage(item2);
		int nx1= img1.getWidth();
		int ny1= img1.getHeight();
		int BitDepth1 = img1.getBitDepth();
		int typ1= img1.getType();
		int NSlices1 = img1.getNSlices();
		int nx2= img2.getWidth();
		int ny2= img2.getHeight();
		int BitDepth2 = img2.getBitDepth();
		int typ2= img2.getType();
		int NSlices2 = img2.getNSlices();

		
		String[] cat ={"GRAY8","GRAY16","GRAY32","COLOR_256","COLOR_RGB"};
		
		//set text
		info.setText( item1 + " "  + "Size: " + " [" + nx1 + " " + ny1 + "]" + " , " + "BitDepth: " + BitDepth1 + " , " + "NSlices: " + NSlices1 + " , " + "Cat : "+ cat[typ1] + "\n");
		info.append( item2 + " " + "Size: " + " [" + nx2 + " " + ny2 + "]" + " , " + "BitDepth: " + BitDepth2 + " , " + "NSlices: " + NSlices2 + " , " + "Cat : "+ cat[typ2] + "\n");
		
		/* v�rifier les entr�es pour d�cocher les loss fonctions non utilisables */
	}

	//Go to Setting dialog box 
	public void actionPerformed(ActionEvent e) {
		setting=set.run("");
		return;
	}
	
}

