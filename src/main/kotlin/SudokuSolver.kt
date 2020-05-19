import kotlin.system.exitProcess
import kotlin.system.measureNanoTime

enum class Column { LEFT, MIDDLE, RIGHT }
enum class Row { TOP, CENTER, BOTTOM }

/**
 * Class representing a Square, which represents a number on the grid.
 */
data class Square(val possibilities: MutableList<Int> = mutableListOf(), var number: Int? = null, val col: Column, val row: Row)

/**
 * Class representing a Cell, the grouping that must contain each number 1-9 once and only once. Contains 9 Squares in a 3x3 square pattern.
 */
data class Cell(val squares: List<Square>, val col: Column, val row: Row) {
    fun getSquare(row: Row, col: Column): Square {
        return squares.find { square -> square.row == row && square.col == col }!!
    }
}

/**
 * Class representing a full Sudoku grid. Contains 9 Cells in a 3x3 square pattern.
 */
data class Grid(val cells: List<Cell>) {
    fun getCell(row: Row, col: Column): Cell {
        return cells.find { cell -> cell.row == row && cell.col == col }!!
    }
}

fun main() {
    val grid = Grid(
        List(9) { i ->
            Cell(
                squares = List(9) { j ->
                    Square(
                        col = Column.values()[j % 3],
                        row = Row.values()[j / 3])
                },
                col = Column.values()[i % 3],
                row = Row.values()[i / 3])
        }
    )
    prepopulateGrid(grid)
    printGrid(grid)
    solve(grid)
    printGrid(grid)
}

fun prepopulateGrid(grid: Grid) {
    println("Enter starting board, using _ for empty squares. Use [enter] to separate rows")
    for(i in 0..8) {
        val ins = readLine()?.split(" ")
        if(ins?.size != 9) exitProcess(2)
        else ins.forEachIndexed { j, num ->
            if(num != "_")
                grid.getCell(Row.values()[i / 3], Column.values()[j / 3]).getSquare(Row.values()[i % 3], Column.values()[j % 3]).number = num.toInt()
        }
    }
}

/**
 * @param grid: The Sudoku grid to solve, with squares with beginning values set and all other squares number set to null
 *
 * Solves the Sudoku grid using a few logical steps
 *
 * Fig 1:                                Fig 2:                                Fig 3:
 *   +---+---+---+++---+---+---+++...      +---+---+---+++---+---+---+++...      +---+---+---+++---+---+---+++...
 *   | A | B | C ||| 1 | 2 | 3 |||         | X | X | ? ||| ? | ? | X |||         | X | X | ? ||| ? | ? | ? |||
 *   +---+---+---+++---+---+---+++...      +---+---+---+++---+---+---+++...      +---+---+---+++---+---+---+++...
 *   | D | E | F ||| 4 | 5 | 6 |||         | ? | ? | ? ||| ? | ? | ? |||         | ? | ? | ? ||| ? | ? | ? |||
 *   +---+---+---+++---+---+---+++...      +---+---+---+++---+---+---+++...      +---+---+---+++---+---+---+++...
 *   | G | H | I ||| 7 | 8 | 9 |||         | ? | ? | ? ||| ? | X | ? |||         | ? | ? | ? ||| ? | X | ? |||
 *   +---+---+---+++---+---+---+++...      +---+---+---+++---+---+---+++...      +---+---+---+++---+---+---+++...
 *   +---+---+---+++---+---+---+++...      .   .   .   ...   .   .   ...         .   .   .   ...   .   .   ...
 *   | 1 | 2 | 3 |||...                    .   .   .   ...   .   .   ...         .   .   .   ...   .   .   ...
 *   +---+---+---+++
 *   | 4 | 5 | 6 |||    ...
 *   +---+---+---+++
 *   | 7 | 8 | 9 |||        ...
 *   +---+---+---+++
 *   .   .   .   ...
 *   .   .   .   ...
 *
 *   Step one: Looking at known values, can a square possibly hold a value?
 *              For example, from Fig 1 we can see that A CANNOT hold the values 1, 2, 3, 4, or 7 as those numbers exist
 *                  in the same row or column of an adjacent cell. A COULD hold the values 5, 6, 8, or 9 as (from the
 *                  information we have), these values DO NOT appear in the same row or column of any adjacent cell.
 *
 *   Step two: Looking at possibilities, can we logically lock out a row/column?
 *              Looking at Fig 2 with ? as values we do not care about and X as a possibility for some number X to
 *                  occupy that square, we can see that it is possible to remove a possibility of X from the right-most
 *                  cell (Fig 3). Since the real location of X MUST be in the top row of the left-most cell, it reasons
 *                  that the real location of X CANNOT be in the top row of the right-most cell.
 *
 *   Step three: Now that we have refined possible values from steps one and two, we can make a few logical conclusions
 *      to determine the true value of a square. If a number X is a possible value in only ONE square of a cell, that
 *      square's value MUST be X, as X cannot be a value of any other square in the cell. Also, if a square has only one
 *      possible value Y, that square's value must be Y as it cannot be anything else.
 *
 *   Step four: Repeating this process continually updates the possibilities for each square and will work towards a
 *      fully solved grid. The grid is complete when all squares have a final value.
 */
fun solve(grid: Grid) {
    val timeToSolve = measureNanoTime {
        while(grid.cells.any { cell -> cell.squares.any { square -> square.number == null } }) {
            // Reset the list of possible values for each square
            grid.cells.forEach { cell -> cell.squares.forEach { square -> square.possibilities.clear() } }

            /*
            Checks if a square can possibly be a value based on KNOWN values. This is the basic Sudoku check,
            does the number already exist in this row or column?
             */
            (1..9).forEach { num ->
                grid.cells.forEach { cell ->
                    cell.squares.filter { square -> square.number == null }.forEach { square ->
                        if(cell.squares.none { s -> s.number == num } &&
                            grid.cells.filter { adjCell ->
                                (adjCell.col == cell.col) xor (adjCell.row == cell.row)
                            }.none { adjCell ->
                                adjCell.squares.any { similarSquare ->
                                    if(adjCell.col == cell.col)
                                        square.col == similarSquare.col && num == similarSquare.number
                                    else
                                        square.row == similarSquare.row && num == similarSquare.number
                                }
                            }) {
                            square.possibilities.add(num)
                        }
                    }
                }
            }

            /*
            A second more advanced check. This check sees if possibilities for a number occupy just one row or column.
            If this is the case, the number for that cell MUST fit in that row or column and thus adjacent cells must
            NOT be able to fit that number in that row or column. This works not off of known numbers but rather "if
            a number will occupy row R, that number is not a possibility for squares on row R in row-adjacent cells".
             */
            (1..9).forEach { num ->
                grid.cells.forEach { cell ->
                    with(cell.squares.filter { square -> square.possibilities.contains(num) }) {
                        if(this.size == 1) {
                            clearLogicallyInvalidFlagsFromCol(grid, cell, num, this.first().col)
                            clearLogicallyInvalidFlagsFromRow(grid, cell, num, this.first().row)
                        } else if(this.size in 2..3) {
                            val testCol = this.first().col
                            val testRow = this.first().row
                            if(this.all { square -> square.col == testCol }) {
                                clearLogicallyInvalidFlagsFromCol(grid, cell, num, testCol)
                            } else if(this.all { square -> square.row == testRow }) {
                                clearLogicallyInvalidFlagsFromRow(grid, cell, num, testRow)
                            }
                        }
                    }
                }
            }

            grid.cells.forEach { cell ->
                // If a number appears as a possibility only once in a cell, that square MUST have that value.
                (1..9).forEach { testSingle ->
                    if(cell.squares.filter { square -> square.possibilities.contains(testSingle) }.size == 1) {
                        cell.squares.find { square -> square.possibilities.contains(testSingle) }!!.apply {
                            possibilities.clear()
                            number = testSingle
                        }
                    }
                }
                // If a square has only one possibility, that MUST be the value of that square
                cell.squares.filter { square ->
                    square.possibilities.size != 0
                }.sortedBy { square ->
                    square.possibilities.size
                }.forEach { square ->
                    if(square.possibilities.size == 1) {
                        square.number = square.possibilities.first()
                        cell.squares.filter { otherSquare -> square != otherSquare }.forEach { otherSquare ->
                            otherSquare.possibilities.remove(square.possibilities.first())
                        }
                        square.possibilities.clear()
                    }
                }
            }
        }
    }

    println("\nSolved in ${timeToSolve / 1_000_000} ms")
}

/**
 * @param grid: The Sudoku grid on which to operate
 * @param cell: The Cell in which the possibilities of a number lie in a row
 * @param num: The number of which it's possibilities in the cell lie in a row
 * @param removeFromRow: The row in which the possibilities lie
 *
 * See step 2 of the solve method header comment
 */
fun clearLogicallyInvalidFlagsFromRow(grid: Grid, cell: Cell, num: Int, removeFromRow: Row) {
    // Clear invalid flags from same-row cells
    grid.cells.filter { adjCell ->
        (adjCell.row == cell.row) and (adjCell.col != cell.col)
    }.forEach { adjCell ->
        adjCell.squares.filter { square ->
            square.possibilities.contains(num)
        }.forEach { square ->
            if(square.row == removeFromRow) {
                square.possibilities.remove(num)
            }
        }
    }
}

/**
 * @param grid: The Sudoku grid on which to operate
 * @param cell: The Cell in which the possibilities of a number lie in a column
 * @param num: The number of which it's possibilities in the cell lie in a column
 * @param removeFromColumn: The column in which the possibilities lie
 *
 * See step 2 of the solve method header comment
 */
fun clearLogicallyInvalidFlagsFromCol(grid: Grid, cell: Cell, num: Int, removeFromColumn: Column) {
    // Clear invalid flags from same-column cells
    grid.cells.filter { adjCell ->
        (adjCell.col == cell.col) and (adjCell.row != cell.row)
    }.forEach { adjCell ->
        adjCell.squares.filter { square ->
            square.possibilities.contains(num)
        }.forEach { square ->
            if(square.col == removeFromColumn) {
                square.possibilities.remove(num)
            }
        }
    }
}

/**
 * @param grid: The grid to print
 */
fun printGrid(grid: Grid) {
    print("+---+---+---+")
    for(i in Row.values()) {
        val gridRow = grid.cells.filter { cell ->
            cell.row == i
        }.sortedBy { cell ->
            cell.col
        }
        for(j in Row.values()) {
            print("\n|")
            gridRow.forEach { cell ->
                cell.squares.filter { square ->
                    square.row == j
                }.sortedBy { square ->
                    square.col
                }.forEach { square ->
                    print(square.number ?: " ")
                }
                print("|")
            }
        }
        if(i != Row.BOTTOM) {
            print("\n+---+---+---+")
        }
    }
    println("\n+---+---+---+")
}