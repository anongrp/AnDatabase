package anon.database;

import anon.database.connect.Connection;
import anon.database.exceptions.ColumnIndexOutOfBoundException;
import anon.database.exceptions.FileTypeNotSupportedException;
import anon.database.exceptions.TableCreationOutOfBoundException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Table {
    private Integer primaryKey;
    private String tbName = "tb-temp";
    public File tbDir;
    private File metaData;
    private Integer objectCallCounter=0;
    private boolean status = false;
    private int noOfColumns;
    private String[] columnNames;
    private HashMap<String,Integer> colInfo = new HashMap<>();
    private ArrayList<String> columnArrayList=new ArrayList<>();
    private ArrayList<String> rowArrayList=new ArrayList<>();
    private BufferedReader importedFileReader;
    private BufferedWriter exportFileWriter;
    private Integer columnItemCounter=0;
    private Integer counterForANDB=0;
    private Integer rCounter=0;
    private Integer rowCounter=0;


    // Constructor
    public Table(String tbName, Database database,String columnNames[]) throws IOException, TableCreationOutOfBoundException {
        createTable(tbName, database, columnNames);
    }

    /* Method For Create Table */
    private void createTable(String tbName, Database database,String columnNames[]) throws IOException {
        this.tbName = "tb-" + tbName;
        tbDir = new File(database.dbDir.getAbsolutePath() + File.separator + this.tbName + ".andb");
        metaData = new File(database.dbDir.getAbsolutePath()+ File.separator + "/MetaData.json");
        if (database.isDatabaseAvailable() && !(tbDir.exists())) {
            tbDir.createNewFile();
            metaData.createNewFile();
            status = true;
        }

        String[] columns = new String[columnNames.length+1];
        columns[0] = "key";
        for (int i=1;i<columns.length;i++){
            columns[i] = columnNames[i-1];
        }
        noOfColumns = columns.length;
        this.columnNames = columns;
        setColInfo(columns);

        if (tbDir.length() == 0){

            BufferedWriter writeColumnData = new BufferedWriter(new FileWriter(tbDir, true));
            for (int i = 0; i < columns.length; i++) {
                writeColumnData.write("¤" + columns[i]);
            }
            writeColumnData.write("¤");
            writeColumnData.close();
        }

    }


    public boolean isTableAvailable(){
        return tbDir.exists();
    }


    public void insertRow(String row[]) throws IOException, ColumnIndexOutOfBoundException {
        String[] finalRow = new String[row.length + 1];
         if (!hasMoreThanOneRow()){
             primaryKey = 1;
             finalRow[0] = primaryKey.toString();
             for (int i=1;i<finalRow.length;i++){
                 finalRow[i] = row[i-1];
             }
             FileWriter metaDataWriter = new FileWriter(metaData);
             metaDataWriter.append("{\n\tKeyInfo:\n\t{\n\t\tlastKey:"+primaryKey+"\n\t}\n}");
             metaDataWriter.close();

         }else {
             BufferedReader reader = new BufferedReader(new FileReader(metaData));
             String data ,key = "0";
             while ((data = reader.readLine()) != null){
                 if (data.contains("lastKey")){
                     key = data.substring(data.indexOf(":")+1,data.length());
                 }
             }
             //ArrayList<String> lastRowData = getFetchedData(lastRow);
             primaryKey = Integer.parseInt(key)+1;

             finalRow[0] = primaryKey.toString();
             for (int i=1;i<finalRow.length;i++){
                 finalRow[i] = row[i-1];
             }

             FileWriter metaDataWriter = new FileWriter(metaData);
             metaDataWriter.append("{\n\tKeyInfo:\n\t{\n\t\tlastKey:"+primaryKey+"\n\t}\n}");
             metaDataWriter.close();
         }

        BufferedWriter writeRowData = new BufferedWriter(new FileWriter(tbDir, true));
        writeRowData.newLine();
        if (noOfColumns < finalRow.length) {
            throw new ColumnIndexOutOfBoundException();
        }
        else if (noOfColumns > finalRow.length){
            Integer blankCell = noOfColumns - finalRow.length;
            for (int i = 0; i < finalRow.length; i++) {
                writeRowData.write("ȸ" + finalRow[i]);
            }
            for (int i=0;i<blankCell;i++){
                writeRowData.write("ȸ" + "Null");
            }
            writeRowData.write("ȸ");
            writeRowData.close();
        }
        else {
            for (int i = 0; i < finalRow.length; i++) {
                writeRowData.write("ȸ" + finalRow[i]);
            }
            writeRowData.write("ȸ");
            writeRowData.close();
        }
    }


    private void setColInfo(String[] colNames){
        for (int i=1;i<=colNames.length;i++){
            colInfo.put(colNames[i-1],i);
        }
    }


    private int counter(String rowData,Character target){
        char[] data = rowData.toCharArray();
        Integer count=0;
        for (int i=0;i<data.length;i++){
            if (data[i]==target){
                count++;
            }
        }
        return count;
    }

    private boolean hasMoreThanOneRow() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(tbDir));
        String data = "";
        Integer counter = 0;
        while (counter != 2){
            data = reader.readLine();
            counter++;
        }
        if (data == null) {
            return false;
        } else {
            return true;
        }
    }

    private ArrayList<String> searcher(String data,String colName) throws IOException {
        Integer rowNo = colInfo.get(colName);
        Integer dataNo;
        BufferedReader readRowData = new BufferedReader(new FileReader(tbDir));
        String rowData,colData;
        ArrayList<String> finalData = new ArrayList<String>();
        while ((rowData = readRowData.readLine()) != null){
            colData = getDataFromTable(rowData,colName);
            if (colData.equals(data)){
                dataNo = counter(rowData.substring(0,rowData.indexOf(data)),'ȸ');
                if (rowNo.equals(dataNo)){
                    finalData.add(rowData);
                }
            }
        }
        return finalData;
    }


    private String getDataFromTable(String row,String colName){
        return getFetchedData(row).get(colInfo.get(colName) - 1);
    }

    public boolean deleteElementWithQuery(String query) throws IOException {
        return deleteElement(query.substring(0,query.indexOf("=")).trim(),query.substring(query.indexOf("=")+1,query.length()).trim());
    }

    public boolean deleteElement(String colName,String target) throws IOException {

        boolean deleteStatus = false;
        String deleteRow = "";
        ArrayList<String> rows = new ArrayList<String>();
        ArrayList<String> finalRow = searcher(target,colName);
        if (!finalRow.isEmpty()){
            deleteRow = finalRow.get(0);
        }
        BufferedWriter tableWriter = new BufferedWriter(new FileWriter(tbDir,true));
        BufferedReader tableReader = new BufferedReader(new FileReader(tbDir));
        String data;
        try {
            while ((data = tableReader.readLine()) != null){
                if (!data.equals(deleteRow)){
                    rows.add(data);
                }
            }
            PrintWriter writer = new PrintWriter(tbDir);
            writer.print("");
            writer.close();
            for (int j=0;j<rows.size();j++){
                if (!(rows.get(j).equals(""))){
                    tableWriter.write(rows.get(j));
                    if (!(rows.size() == j+1)){
                        tableWriter.newLine();
                    }
                }
            }
            deleteStatus = true;
        }catch (Exception e){
            deleteStatus = false;
        }finally {
            tableReader.close();
            tableWriter.close();
            return deleteStatus;
        }
    }


    private ArrayList<String> getFetchedData(String fullRow){
        ArrayList<Integer> symbolPositions = new ArrayList<Integer>();
        ArrayList<String> fechedData = new ArrayList<String>();
        if (fullRow != null){
            char[] dataArray = fullRow.toCharArray();
            for (int i=0;i<dataArray.length;i++){
                if (dataArray[i] == 'ȸ' || dataArray[i] == '¤'){
                    symbolPositions.add(i);
                }
            }
            try {
                for (int j=0;j<symbolPositions.size();j++){
                    fechedData.add(fullRow.substring(symbolPositions.get(j)+1,symbolPositions.get(j+1)));
                }
            }catch (Exception ignored){
            }
        }else {
            fechedData.add(null);
        }
        return fechedData;
    }


    public ArrayList<String> getRowWithQuery(String query) throws IOException {
        return getRow(query.substring(0, query.indexOf("=")).trim(),query.substring(query.indexOf("=")+1,query.length()).trim());
    }

    public ArrayList<String> getRow(String colName,String target) throws IOException {
        String row = null;
        try {
            row = searcher(target,colName).get(0);
        }catch (Exception ignored){
        }finally {
            return getFetchedData(row);
        }
    }


    public ArrayList<ArrayList<String>> getTable(Integer limit) throws IOException {
        ArrayList<ArrayList<String>> tableData = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(tbDir));
        String rowData;
        for (int i=0;i<limit;i++){
            rowData = reader.readLine();
            tableData.add(getFetchedData(rowData));
        }
        return tableData;
    }


    public ArrayList<ArrayList<String>> getFullTable() throws IOException {
        ArrayList<ArrayList<String>> tableData = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(tbDir));
        String rowData;
        while ((rowData = reader.readLine()) != null){
            tableData.add(getFetchedData(rowData));
        }
        return tableData;
    }

    private void updateCell(String colName,String colData,String updateCol,String updateData) throws IOException {
        String updateRow = "";
        ArrayList<String> rows = new ArrayList<String>();
        ArrayList<String> finalRow = searcher(colData,colName);
        if (!finalRow.isEmpty()){
            updateRow = finalRow.get(0);
        }
        BufferedWriter tableWriter = new BufferedWriter(new FileWriter(tbDir,true));
        BufferedReader tableReader = new BufferedReader(new FileReader(tbDir));
        String data;
        try {
            while ((data = tableReader.readLine()) != null){
                if (data.equals(updateRow)){
                    ArrayList<String> updatedRow = getFetchedData(data);
                    data = "";
                    updatedRow.remove(colInfo.get(updateCol)-1);
                    updatedRow.add(colInfo.get(updateCol)-1,updateData);
                    for (String cellData:updatedRow){
                        data = data+"ȸ"+cellData;
                    }
                    data = data + "ȸ";
                    rows.add(data);
                }else {
                    rows.add(data);
                }
            }
            PrintWriter writer = new PrintWriter(tbDir);
            writer.print("");
            writer.close();
            for (int j=0;j<rows.size();j++){
                if (!(rows.get(j).equals(""))){
                    tableWriter.write(rows.get(j));
                    if (!(rows.size() == j+1)){
                        tableWriter.newLine();
                    }
                }
            }
        }catch (Exception e){

        }finally {
            tableReader.close();
            tableWriter.close();
        }
    }


    public String execQuery(String query) throws IOException {
        String feedback = "false";
        if (query.contains("get")){
            if (query.contains("where") && query.contains("=")){
                String dataCol,colName,colData;
                dataCol = query.substring(query.indexOf(" ",query.indexOf("get")),query.indexOf(" ",query.indexOf(" ",query.indexOf("get"))+1)).trim();
                colName = query.substring(query.indexOf(" ",query.indexOf("where")),query.indexOf("=")).trim();
                colData = query.substring(query.indexOf("=")+1,query.length()).trim();
                feedback =  getDataFromTable(searcher(colData,colName).get(0),dataCol);
            }
        }else if (query.contains("update")){
            String data,dataCol,colName,colData;
            dataCol = query.substring(query.indexOf(" ",query.indexOf("update")),query.indexOf("=",query.indexOf(" ",query.indexOf("update")))).trim();
            data = query.substring(query.indexOf("=")+1,query.indexOf(" ",query.indexOf("=")+1)).trim();
            colName = query.substring(query.indexOf(" ",query.indexOf("where")+1),query.indexOf("=",query.indexOf("where"))).trim();
            colData = query.substring(query.indexOf("=",query.indexOf("where"))+1,query.length()).trim();
            updateCell(colName,colData,dataCol,data);
            feedback = "true";
        }else {
            feedback = "false";
        }

        return feedback;
    }


    public boolean exportToCSV(File csvFile) throws IOException {
        boolean status = false;
        final String COMMA_DELIMITER = ",";
        final String NEW_LINE_SEPARATOR = "\n";

        ArrayList<ArrayList<String>> tableData = getFullTable();

        FileWriter writer = null;
        try {
            csvFile.createNewFile();
            writer = new FileWriter(csvFile);
            Integer counter = -1;
            for (Object i : tableData){
                counter++;
                for (Object j : tableData.get(counter)){
                    writer.append(j.toString());
                    writer.append(COMMA_DELIMITER);
                }
                writer.append(NEW_LINE_SEPARATOR);
            }
            status = true;
            writer.close();
        }catch (Exception e){
            status = false;
        }
        return status;
    }

    public boolean exportToJSON(File jsonFile) throws IOException {
        boolean status = false;
        final String COMMA_DELIMITER = ",";
        final String NEW_LINE_SEPARATOR = "\n";
        final String DATA_SEPARATOR = ":";
        final String BRACKET_OPEN = "{";
        final String BRACKET_CLOSE = "}";
        final String SINGLE_TAB = "\t";
        final String DOUBLE_TAB = "\t\t";

        ArrayList<ArrayList<String>> tableData = getFullTable();
        FileWriter writer = null;
        Integer tempCounter = 0;
        try {
            jsonFile.createNewFile();
            writer = new FileWriter(jsonFile);
            writer.append(BRACKET_OPEN+NEW_LINE_SEPARATOR+SINGLE_TAB+'"'+tbName.substring(tbName.indexOf("-")+1,tbName.length())+'"'+": ["+NEW_LINE_SEPARATOR);

            for (ArrayList<String> row:tableData){
                writer.append(SINGLE_TAB+BRACKET_OPEN);
                for (int i=0;i<row.size();i++){
                    if (i+1 < row.size()){
                        writer.append(SINGLE_TAB+NEW_LINE_SEPARATOR+DOUBLE_TAB+'"'+columnNames[i]+'"'+DATA_SEPARATOR+'"'+row.get(i)+'"'+COMMA_DELIMITER);
                    }
                    else {
                        writer.append(SINGLE_TAB+NEW_LINE_SEPARATOR+DOUBLE_TAB+'"'+columnNames[i]+'"'+DATA_SEPARATOR+'"'+row.get(i)+'"');
                    }
                }
                if (tempCounter+1 < tableData.size()){
                    writer.append(NEW_LINE_SEPARATOR+SINGLE_TAB+BRACKET_CLOSE+COMMA_DELIMITER+NEW_LINE_SEPARATOR);
                }else {
                    writer.append(NEW_LINE_SEPARATOR+SINGLE_TAB+BRACKET_CLOSE);
                }
                status = true;
                tempCounter++;
            }

            writer.append("]"+NEW_LINE_SEPARATOR+BRACKET_CLOSE);

        }catch (Exception e){
            status = false;
        }finally {
            writer.close();
        }

        return status;
    }

    public boolean exportToXML(File xmlFile) throws IOException {
        boolean status = false;
        final String TAG_OPENING = "<";
        final String TAG_CLOSING = ">";
        final String TAG_OPENING_WITH_CLOSE = "</";
        final String NEW_LINE_SEPARATOR = "\n";
        final String SINGLE_TAB = "\t";
        final String DOUBLE_TAB = "\t\t";
        final String XMLVERSION_AND_ENCODING="<?xml version=\"1.0\" encoding=\"utf-8\" ?>";
        ArrayList<ArrayList<String>> tabelData = this.getFullTable();
        FileWriter writer = null;
        Integer count = 0;
        try {
            xmlFile.createNewFile();
            writer = new FileWriter(xmlFile);
            writer.append(XMLVERSION_AND_ENCODING+NEW_LINE_SEPARATOR);
            writer.append(TAG_OPENING+"Table"+TAG_CLOSING);
            for (ArrayList<String> row: tabelData){
                count++;
                if(count == 1){
                    writer.append(NEW_LINE_SEPARATOR+SINGLE_TAB+TAG_OPENING+"Column key="+row.get(0)+TAG_CLOSING+NEW_LINE_SEPARATOR);
                    for (int i=1;i<row.size();i++){
                        writer.append(DOUBLE_TAB+TAG_OPENING+"data"+TAG_CLOSING+row.get(i)+TAG_OPENING_WITH_CLOSE+"data"+TAG_CLOSING+NEW_LINE_SEPARATOR);
                    }
                    writer.append(SINGLE_TAB+TAG_OPENING_WITH_CLOSE+"Column"+TAG_CLOSING);
                }
                if (count != 1){
                    writer.append(NEW_LINE_SEPARATOR+SINGLE_TAB+TAG_OPENING+"Row key="+row.get(0)+TAG_CLOSING+NEW_LINE_SEPARATOR);
                    for (int i=1;i<row.size();i++){
                        writer.append(DOUBLE_TAB+TAG_OPENING+"data"+TAG_CLOSING+row.get(i)+TAG_OPENING_WITH_CLOSE+"data"+TAG_CLOSING+NEW_LINE_SEPARATOR);
                    }
                    writer.append(SINGLE_TAB+TAG_OPENING_WITH_CLOSE+"Row"+TAG_CLOSING);
                }
                status = true;
            }
            writer.append(NEW_LINE_SEPARATOR+TAG_OPENING_WITH_CLOSE+"Table"+TAG_CLOSING);
        }catch (Exception e){
            status = false;
        }finally {
            writer.close();
        }

        return status;
    }


    @Override
    public String toString() {
        ArrayList<ArrayList<String>> list = null;
        try {
            list = this.getFullTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (list != null){
            for (ArrayList data : list){
                System.out.println("   "+data);
            }
        }
        return "\n\n"+super.toString();
    }

    public boolean importInANDB(String pathname) throws FileTypeNotSupportedException, IOException {
        Boolean status=false;
        final String XML_FILE=".xml";
        final String JSON_FILE=".json";
        final String CSV_FILE=".csv";
        String lineReader=new String();
        if(pathname.contains(XML_FILE)){
            final String COLUMN_OPEN_TAG="<Column key=";
            final String COLUMN_CLOSE_TAG="</Column>";
            final String ROW_OPEN_TAG="<Row key";
            final String ROW_CLOSE_TAG="</Row>";
            final String CLOSE_BRACKET=">";
            final String OPEN_DATA_TAG="<data>";
            final String CLOSE_DATA_TAG="</data>";
            importedFileReader=new BufferedReader(new FileReader(new File(pathname)));
            while (importedFileReader.readLine()!=null){
                lineReader=importedFileReader.readLine();
                System.out.println("It works");
                if (lineReader.contains(COLUMN_OPEN_TAG)){
                    columnArrayList.add(lineReader.substring(lineReader.indexOf(COLUMN_OPEN_TAG)+1,lineReader.indexOf(CLOSE_BRACKET)));
                    columnItemCounter++;
                    while (!lineReader.equals(COLUMN_CLOSE_TAG)){
                        columnArrayList.add(lineReader.substring(lineReader.indexOf(OPEN_DATA_TAG)+1,lineReader.indexOf(CLOSE_DATA_TAG)));
                        columnItemCounter++;
                    }
                }
                else if (lineReader.contains(ROW_OPEN_TAG)){
                    rowArrayList.add(lineReader.substring(lineReader.indexOf(ROW_OPEN_TAG)+1,lineReader.indexOf(CLOSE_BRACKET)));
                    while (!lineReader.equals(ROW_CLOSE_TAG)){
                        rowArrayList.add(lineReader.substring(lineReader.indexOf(OPEN_DATA_TAG)+1,lineReader.indexOf(CLOSE_DATA_TAG)));
                    }
                }
            }
            importedFileReader.close();
            String dbLocation= Connection.dbLocation;
            status=forDataWriting(dbLocation);
        }
        else if (pathname.contains(JSON_FILE)){

        }
        else if (pathname.contains(CSV_FILE)){

        }
        else {
            throw new FileTypeNotSupportedException();
        }
        return status;
    }
    private boolean forDataWriting(String dbPath) throws IOException {
        boolean status=false;
        File andbFile=new File(dbPath+"export"+counterForANDB+".andb");
        counterForANDB++;
        status=columnWriter(andbFile);
        if (status){
            status=rowWriter(andbFile);
            if (status){
                columnArrayList.clear();
                rowArrayList.clear();
            }
        }

    return status;
    }

    private boolean rowWriter(File andbFile) throws IOException {
        boolean status=false;
        final String ROW_INDICATOR="ȸ";
        exportFileWriter=new BufferedWriter(new FileWriter(andbFile,true));
        exportFileWriter.write(ROW_INDICATOR);
        for (int i=rCounter;i<=rowArrayList.size();i++){
            rCounter++;
            rowCounter++;
            if (rowCounter<=columnItemCounter){
                exportFileWriter.write(i+ROW_INDICATOR);
            }
            else {
                exportFileWriter.newLine();
                exportFileWriter.close();
                rowCounter=0;
                rowWriter(andbFile);
            }
        }
        status=true;
        return status;
    }

    private boolean columnWriter(File andbFile) throws IOException {
        boolean status=false;
        final String COLUMN_INDICATOR="¤";
        exportFileWriter=new BufferedWriter(new FileWriter(andbFile));
        exportFileWriter.write(COLUMN_INDICATOR);
        for (int i=0;i<=columnArrayList.size();i++){
            exportFileWriter.write(i+COLUMN_INDICATOR);
        }
        exportFileWriter.newLine();
        exportFileWriter.close();
        status=true;
        return status;
    }
}
