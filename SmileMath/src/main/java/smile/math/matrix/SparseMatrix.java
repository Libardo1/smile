/*******************************************************************************
 * Copyright (c) 2010 Haifeng Li
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package smile.math.matrix;

import java.util.Arrays;
import smile.math.Math;

/**
 * A sparse matrix is a matrix populated primarily with zeros. Conceptually,
 * sparsity corresponds to systems which are loosely coupled. Huge sparse
 * matrices often appear when solving partial differential equations.
 * <p>
 * Operations using standard dense matrix structures and algorithms are slow
 * and consume large amounts of memory when applied to large sparse matrices.
 * Indeed, some very large sparse matrices are infeasible to manipulate with
 * the standard dense algorithms. Sparse data is by nature easily compressed,
 * and this compression almost always results in significantly less computer
 * data storage usage.
 * <p>
 * This class employs Harwell-Boeing column-compressed sparse matrix format.
 * Nonzero values are stored in an array (top-to-bottom, then left-to-right-bottom).
 * The row indices corresponding to the values are also stored. Besides, a list
 * of pointers are indexes where each column starts. This format is efficient
 * for arithmetic operations, column slicing, and matrix-vector products.
 * One typically uses SparseDataset for construction of SparseMatrix.
 *
 * @author Haifeng Li
 */
public class SparseMatrix implements IMatrix {
    /**
     * The number of rows.
     */
    private int nrows;
    /**
     * The number of columns.
     */
    private int ncols;
    /**
     * The index of the start of columns.
     */
    private int[] colIndex;
    /**
     * The row indices of nonzero values.
     */
    private int[] rowIndex;
    /**
     * The array of nonzero values stored column by column.
     */
    private double[] x;

    /**
     * Constructor.
     * @param nrows the number of rows in the matrix.
     * @param ncols the number of columns in the matrix.
     * @param size the number of nonzero entries in the matrix.
     */
    private SparseMatrix(int nrows, int ncols, int nvals) {
        this.nrows = nrows;
        this.ncols = ncols;
        rowIndex = new int[nvals];
        colIndex = new int[ncols + 1];
        x = new double[nvals];
    }

    /**
     * Constructor.
     * @param nrows the number of rows in the matrix.
     * @param ncols the number of columns in the matrix.
     * @param rowIndex the row indices of nonzero values.
     * @param colIndex the index of the start of columns.
     * @param x the array of nonzero values stored column by column.
     */
    public SparseMatrix(int nrows, int ncols, double[] x, int[] rowIndex, int[] colIndex) {
        this.nrows = nrows;
        this.ncols = ncols;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.x = x;
    }

    /**
     * Constructor.
     * @param D a dense matrix to converted into sparse matrix format.
     */
    public SparseMatrix(double[][] D) {
        this(D, 100 * Math.EPSILON);
    }

    /**
     * Constructor.
     * @param D a dense matrix to converted into sparse matrix format.
     * @param tol the tolerance to regard a value as zero if |x| &lt; tol.
     */
    public SparseMatrix(double[][] D, double tol) {
        nrows = D.length;
        ncols = D[0].length;

        int n = 0; // number of non-zero elements
        for (int i = 0; i < nrows; i++) {
            for (int j = 0; j < ncols; j++) {
                if (Math.abs(D[i][j]) >= tol) {
                    n++;
                }
            }
        }

        x = new double[n];
        rowIndex = new int[n];
        colIndex = new int[ncols + 1];
        colIndex[ncols] = n;

        n = 0;
        for (int j = 0; j < ncols; j++) {
            colIndex[j] = n;
            for (int i = 0; i < nrows; i++) {
                if (Math.abs(D[i][j]) >= tol) {
                    rowIndex[n] = i;
                    x[n] = D[i][j];
                    n++;
                }
            }
        }
    }

    @Override
    public int nrows() {
        return nrows;
    }

    @Override
    public int ncols() {
        return ncols;
    }

    /**
     * Returns the number of nonzero values.
     */
    public int size() {
        return colIndex[ncols];
    }

    /**
     * Returns all nonzero values.
     * @return all nonzero values
     */
    public double[] values() {
        return x;
    }
    
    @Override
    public double get(int i, int j) {
        if (i < 0 || i >= nrows || j < 0 || j >= ncols) {
            throw new IllegalArgumentException("Invalid index: i = " + i + " j = " + j);
        }

        for (int k = colIndex[j]; k < colIndex[j + 1]; k++) {
            if (rowIndex[k] == i) {
                return x[k];
            }
        }

        return 0.0;
    }

    @Override
    public void set(int i, int j, double x) {
        if (i < 0 || i >= nrows || j < 0 || j >= ncols) {
            throw new IllegalArgumentException("i = " + i + " j = " + j);
        }

        for (int k = colIndex[j]; k < colIndex[j + 1]; k++) {
            if (rowIndex[k] == i) {
                this.x[k] = x;
                return;
            }
        }

        throw new IllegalArgumentException("SparseMatrix does not support chnage zero values to non-zeros.");
    }

    @Override
    public void ax(double[] x, double[] y) {
        Arrays.fill(y, 0.0);

        for (int j = 0; j < ncols; j++) {
            for (int i = colIndex[j]; i < colIndex[j + 1]; i++) {
                y[rowIndex[i]] += this.x[i] * x[j];
            }
        }
    }

    @Override
    public void axpy(double[] x, double[] y) {
        for (int j = 0; j < ncols; j++) {
            for (int i = colIndex[j]; i < colIndex[j + 1]; i++) {
                y[rowIndex[i]] += this.x[i] * x[j];
            }
        }
    }

    @Override
    public void axpy(double[] x, double[] y, double b) {
        for (int i = 0; i < y.length; i++) {
            y[i] *= b;
        }

        for (int j = 0; j < ncols; j++) {
            for (int i = colIndex[j]; i < colIndex[j + 1]; i++) {
                y[rowIndex[i]] += this.x[i] * x[j];
            }
        }
    }

    @Override
    public void atx(double[] x, double[] y) {
        Arrays.fill(y, 0.0);
        for (int i = 0; i < ncols; i++) {
            for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
                y[i] += this.x[j] * x[rowIndex[j]];
            }
        }
    }


    @Override
    public void atxpy(double[] x, double[] y) {
        for (int i = 0; i < ncols; i++) {
            for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
                y[i] += this.x[j] * x[rowIndex[j]];
            }
        }
    }

    @Override
    public void atxpy(double[] x, double[] y, double b) {
        for (int i = 0; i < ncols; i++) {
            y[i] *= b;
            for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
                y[i] += this.x[j] * x[rowIndex[j]];
            }
        }
    }

    /**
     * Returns the matrix transpose.
     */
    public SparseMatrix transpose() {
        int m = nrows, n = ncols;
        SparseMatrix at = new SparseMatrix(n, m, x.length);

        int[] count = new int[m];
        for (int i = 0; i < n; i++) {
            for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
                int k = rowIndex[j];
                count[k]++;
            }
        }

        for (int j = 0; j < m; j++) {
            at.colIndex[j + 1] = at.colIndex[j] + count[j];
        }

        Arrays.fill(count, 0);
        for (int i = 0; i < n; i++) {
            for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
                int k = rowIndex[j];
                int index = at.colIndex[k] + count[k];
                at.rowIndex[index] = i;
                at.x[index] = x[j];
                count[k]++;
            }
        }

        return at;
    }

    /**
     * Returns the matrix multiplication C = A * B.
     */
    public SparseMatrix times(SparseMatrix B) {
        if (ncols != B.nrows) {
            throw new IllegalArgumentException(String.format("Matrix dimensions do not match for matrix multiplication: %d x %d vs %d x %d", nrows(), ncols(), B.nrows(), B.ncols()));
        }

        int m = nrows;
        int anz = size();
        int n = B.ncols;
        int[] Bp = B.colIndex;
        int[] Bi = B.rowIndex;
        double[] Bx = B.x;
        int bnz = Bp[n];

        int[] w = new int[m];
        double[] abj = new double[m];

        int nzmax = Math.max(anz + bnz, m);

        SparseMatrix C = new SparseMatrix(m, n, nzmax);
        int[] Cp = C.colIndex;
        int[] Ci = C.rowIndex;
        double[] Cx = C.x;

        int nz = 0;
        for (int j = 0; j < n; j++) {
            if (nz + m > nzmax) {
                nzmax = 2 * nzmax + m;
                double[] Cx2 = new double[nzmax];
                int[] Ci2 = new int[nzmax];
                System.arraycopy(Ci, 0, Ci2, 0, nz);
                System.arraycopy(Cx, 0, Cx2, 0, nz);
                Ci = Ci2;
                Cx = Cx2;
                C.rowIndex = Ci;
                C.x = Cx;
            }

            // column j of C starts here
            Cp[j] = nz;

            for (int p = Bp[j]; p < Bp[j + 1]; p++) {
                nz = scatter(this, Bi[p], Bx[p], w, abj, j + 1, C, nz);
            }

            for (int p = Cp[j]; p < nz; p++) {
                Cx[p] = abj[Ci[p]];
            }
        }

        // finalize the last column of C
        Cp[n] = nz;

        return C;
    }

    /**
     * x = x + beta * A(:,j), where x is a dense vector and A(:,j) is sparse.
     */
    private static int scatter(SparseMatrix A, int j, double beta, int[] w, double[] x, int mark, SparseMatrix C, int nz) {
        int[] Ap = A.colIndex;
        int[] Ai = A.rowIndex;
        double[] Ax = A.x;

        int[] Ci = C.rowIndex;
        for (int p = Ap[j]; p < Ap[j + 1]; p++) {
            int i = Ai[p];                // A(i,j) is nonzero
            if (w[i] < mark) {
                w[i] = mark;              // i is new entry in column j
                Ci[nz++] = i;             // add i to pattern of C(:,j)
                x[i] = beta * Ax[p];      // x(i) = beta*A(i,j)
            } else {
                x[i] += beta * Ax[p];     // i exists in C(:,j) already
            }
        }

        return nz;
    }

    /**
     * Returns A * A<sup>T</sup>.
     */
    public static SparseMatrix AAT(SparseMatrix A, SparseMatrix AT) {
        if (A.ncols != AT.nrows) {
            throw new IllegalArgumentException(String.format("Matrix dimensions do not match for matrix multiplication: %d x %d vs %d x %d", A.nrows(), A.ncols(), AT.nrows(), AT.ncols()));
        }

        int m = AT.ncols;
        int[] done = new int[m];
        for (int i = 0; i < m; i++) {
            done[i] = -1;
        }

        // First pass determines the number of nonzeros.
        int nvals = 0;
        // Outer loop over columns of A' in AA'
        for (int j = 0; j < m; j++) {
            for (int i = AT.colIndex[j]; i < AT.colIndex[j + 1]; i++) {
                int k = AT.rowIndex[i];
                for (int l = A.colIndex[k]; l < A.colIndex[k + 1]; l++) {
                    int h = A.rowIndex[l];
                    // Test if contribution already included.
                    if (done[h] != j) {
                        done[h] = j;
                        nvals++;
                    }
                }
            }
        }

        SparseMatrix aat = new SparseMatrix(m, m, nvals);

        nvals = 0;
        for (int i = 0; i < m; i++) {
            done[i] = -1;
        }

        // Second pass determines columns of adat. Code is identical to first
        // pass except colIndex and rowIndex get assigned at appropriate places.
        for (int j = 0; j < m; j++) {
            aat.colIndex[j] = nvals;
            for (int i = AT.colIndex[j]; i < AT.colIndex[j + 1]; i++) {
                int k = AT.rowIndex[i];
                for (int l = A.colIndex[k]; l < A.colIndex[k + 1]; l++) {
                    int h = A.rowIndex[l];
                    if (done[h] != j) {
                        done[h] = j;
                        aat.rowIndex[nvals] = h;
                        nvals++;
                    }
                }
            }
        }

        // Set last value.
        aat.colIndex[m] = nvals;

        // Sort columns.
        for (int j = 0; j < m; j++) {
            if (aat.colIndex[j + 1] - aat.colIndex[j] > 1) {
                Arrays.sort(aat.rowIndex, aat.colIndex[j], aat.colIndex[j + 1]);
            }
        }

        double[] temp = new double[m];
        for (int i = 0; i < m; i++) {
            for (int j = AT.colIndex[i]; j < AT.colIndex[i + 1]; j++) {
                int k = AT.rowIndex[j];
                for (int l = A.colIndex[k]; l < A.colIndex[k + 1]; l++) {
                    int h = A.rowIndex[l];
                    temp[h] += AT.x[j] * A.x[l];
                }
            }

            for (int j = aat.colIndex[i]; j < aat.colIndex[i + 1]; j++) {
                int k = aat.rowIndex[j];
                aat.x[j] = temp[k];
                temp[k] = 0.0;
            }
        }

        return aat;
    }

    /**
     * Returns the largest eigen pair of matrix with the power iteration
     * under the assumptions A has an eigenvalue that is strictly greater
     * in magnitude than its other its other eigenvalues and the starting
     * vectorhas a nonzero component in the direction of an eigenvector
     * associated with the dominant eigenvalue.
     * @param v on input, it is the non-zero initial guess of the eigen vector.
     * On output, it is the eigen vector corresponding largest eigen value.
     * @return the largest eigen value.
     */
    public double eigen(double[] v) {
        if (nrows != ncols) {
            throw new UnsupportedOperationException("The matrix is not square.");
        }

        return EigenValueDecomposition.eigen(this, v);
    }

    /**
     * Returns the k largest eigen pairs. Only works for symmetric matrix.
     */
    public EigenValueDecomposition eigen(int k) {
        if (nrows != ncols) {
            throw new UnsupportedOperationException("The matrix is not square.");
        }

        return EigenValueDecomposition.decompose(this, k);
    }

    @Override
    public void asolve(double[] b, double[] x) {
        for (int i = 0; i < nrows; i++) {
            double diag = 0.0;
            for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
                if (rowIndex[j] == i) {
                    diag = this.x[j];
                    break;
                }
            }
            x[i] = diag != 0.0 ? b[i] / diag : b[i];
        }
    }
}
