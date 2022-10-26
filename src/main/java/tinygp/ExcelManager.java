package tinygp;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ExcelManager {
    private String filename;
    public void setFilename(String filename){
        File file = new File(filename);
        int index = file.getName().lastIndexOf(".");
        this.filename = file.getName().substring(0, index) + ".xlsx";
    }

    private double[][] targets;
    private final List<String> bestIndividuals;

    private int inputsCount;
    private void setInputCount(){
        inputsCount = targets[0].length;
    }

    private int bestIndividualsCount;
    private void setBestIndividualsCount(){
        bestIndividualsCount = bestIndividuals.size();
    }

    public ExcelManager() {
        bestIndividuals = new ArrayList<>();
    }

    public void setTargets(double[][] targets) {
        this.targets = targets;
        setInputCount();
    }

    public void addBestIndividual(String bestIndividual){
        bestIndividuals.add(bestIndividual);
        setBestIndividualsCount();
    }

    public void writeToExcel() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("TinyGP");

        setupHeader(sheet);
        for(int i = 1; i <= targets.length; i++)
            setupDataRow(sheet, i);

        try{
            FileOutputStream fileOut = new FileOutputStream("results/" + filename);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
        } catch (IOException e){
            System.out.println("ERROR: Writing to excel failed with exception: " + e.getMessage());
            System.exit(0);
        }
    }

    private void setupHeader(Sheet sheet){
        Row header = sheet.createRow(0);

        int cellIterator = 0;
        for(int i = 1; i < inputsCount; i++) {
            sheet.setColumnWidth(cellIterator, 4000);
            header.createCell(cellIterator++).setCellValue("X" + i);
        }

        header.createCell(cellIterator).setCellValue("Target");

        for(int i = 0; i < bestIndividualsCount; i++) {
            header.createCell(++cellIterator).setCellValue("Gen " + i + " best individual");
            sheet.setColumnWidth(cellIterator, 6000);
        }
    }

    private void setupDataRow(Sheet sheet, int rowIndex){
        Row row = sheet.createRow(rowIndex);

        int cellIterator = 0;
        for(int i = 0; i < inputsCount; i++)
            row.createCell(cellIterator++).setCellValue(targets[rowIndex - 1][i]);


        for(int i = 0; i < bestIndividualsCount; i++) {
            String formula = bestIndividuals.get(i);
            for(int j = 1; j < inputsCount; j++){
                String colName = CellReference.convertNumToColString(j - 1);
                formula = formula.replace(("x" + j), (colName + (rowIndex + 1)));
            }

            row.createCell(cellIterator++, CellType.FORMULA).setCellFormula(formula);
        }
    }
}
