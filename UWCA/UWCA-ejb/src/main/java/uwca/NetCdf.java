/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uwca;




import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 *
 * @author jwoodrin
 */
public class NetCdf {
    
    public void writeToeFile(String filename, int x, int y, int[][] values){
  // We are writing 2D data, a 6 x 12 grid.
//    final int NX = 6;
//    final int NY = 12;

  //  String filename = "simple_xy.nc";
    NetcdfFileWriter dataFile = null;

    try {
      dataFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, filename);

      // Create netCDF dimensions,
      Dimension xDim = dataFile.addDimension(null, "x", x);
      Dimension yDim = dataFile.addDimension(null, "y", y);

      // define dimensions
      List<Dimension> dims = new ArrayList<>();
      dims.add(xDim);
      dims.add(yDim);

      // Define a netCDF variable. The type of the variable in this case
      // is ncInt (32-bit integer).
      Variable dataVariable = dataFile.addVariable(null, "toe", DataType.INT, dims);

      // create the file
      dataFile.create();

      // This is the data array we will write. It will just be filled
      // with a progression of numbers for this example.
      ArrayInt.D2 dataOut = new ArrayInt.D2(xDim.getLength(), yDim.getLength());

      // Create some pretend data. If this wasn't an example program, we
      // would have some real data to write, for example, model output.
      int i, j;

      for (i = 0; i < xDim.getLength(); i++) {
        for (j = 0; j < yDim.getLength(); j++) {
        //  dataOut.set(i, j, i * NY + j);
            dataOut.set(i, j, values[i][j]);
        }
      }

      // Write the pretend data to the file. Although netCDF supports
      // reading and writing subsets of data, in this case we write all
      // the data in one operation.
      dataFile.write(dataVariable, dataOut);

    } catch (IOException e) {
      e.printStackTrace();

    } catch (InvalidRangeException e) {
      e.printStackTrace();

    } finally {
      if (null != dataFile)
        try {
          dataFile.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
    }

    System.out.println("*** SUCCESS writing example file simple_xy.nc!");
     
    }
    
}
