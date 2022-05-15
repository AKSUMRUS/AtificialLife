package com.aksum

import com.aksum.Main.*
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array

class Molecule(){
    lateinit var genome: Array<Int>
    var energy: Int = 100
    var direction: Int = 0
    var pointer: Int = 0
    var x: Int = 0
    var y: Int = 0
    var isAlive: Boolean = true

    constructor(_x: Int,_y: Int,_genome: Array<Int>): this(){
        x = _x
        y = _y
        genome = _genome
    }

    constructor(_x: Int,_y: Int,_genome: Array<Int>,_energy: Int): this(){
        x = _x
        y = _y
        genome = _genome
        energy = _energy
    }

    fun makeMove(){
        var cnt = 0
        while (true) {
            ++cnt
            if(cnt > 20){
                return
            }
            energy = Math.min(700,energy)
            --energy
            if(energy <= 0){
                isAlive = false
            }
            if (genome[pointer] == 0 || energy >= 600) {
                var newEnergy = energy / 5
                var newLikelihood = 5
                if (!divide(newEnergy,newLikelihood) || newEnergy <= 20) {
                    isAlive = false
                    return
                }
                else{
                    energy -= newEnergy
                }
                if (divide(newEnergy,newLikelihood)) {
                    energy -= newEnergy
                }
                if (divide(newEnergy,newLikelihood)) {
                    energy -= newEnergy
                }
                if (divide(newEnergy,newLikelihood)) {
                    energy -= newEnergy
                }
                return
            } else if (genome[pointer] == 1) {
                turn(genome[(pointer + 1) % GENOME_SIZE]);
            } else if (genome[pointer] == 22) {
                photosynthesis()
                return
            } else if (genome[pointer] == 62){
                move(genome[(pointer + 1) % GENOME_SIZE])
                return
            }
            else if(genome[pointer] == 59){
                eatOrganic()
                return
            }
            else if(genome[pointer] == 53){
                eatEnemy(genome[(pointer + 1)% GENOME_SIZE])
                return
            }
            else{
                pointer = (pointer+genome[pointer]) % GENOME_SIZE
            }
        }
    }

    fun divide(_energy: Int,_likelihood: Int = 0): Boolean{
        var newGenome:Array<Int> = Array()
        var likelihood = MathUtils.random(1,100)

        for(i in 0 until GENOME_SIZE){
            newGenome.add(genome[i])
        }

        pointer = (pointer+1)%GENOME_SIZE

        if(likelihood <= _likelihood){
            var pos = MathUtils.random(0, GENOME_SIZE-1)
            likelihood = MathUtils.random(1,6)
            var newVal = 0
            if(likelihood == 1){
                newVal = 1
            }
            else if(likelihood == 2){
                newVal = 62
            }
            else if(likelihood == 3){
                newVal = 22
            }
            else if(likelihood == 4){
                newVal = 53
            }
            else if(likelihood == 5){
                newVal = 59
            }
            else if(likelihood == 6){
                newVal = MathUtils.random(0, GENOME_SIZE-1)
            }
//            newVal = MathUtils.random(0, GENOME_SIZE-1)
            newGenome[pos] = newVal
        }
        if(field[(x+FIELD_WIDTH-1)% FIELD_WIDTH][y] == -1){
            field[(x+FIELD_WIDTH-1)% FIELD_WIDTH][y] = molecules.size
            molecules.add(Molecule((x+FIELD_WIDTH-1)% FIELD_WIDTH,y,newGenome,_energy))
            return true
        }
        if(field[(x+1)% FIELD_WIDTH][y] == -1){
            field[(x+1)% FIELD_WIDTH][y] = molecules.size
            molecules.add(Molecule((x+1)% FIELD_WIDTH,y,newGenome,_energy))
            return true
        }
        if(y > 0){
            if(field[x][y-1] == -1){
                field[x][y-1] = molecules.size
                molecules.add(Molecule(x,y-1,newGenome,_energy))
                return true
            }
        }
        if(y < FIELD_HEIGHT - 1){
            if(field[x][y+1] == -1){
                field[x][y+1] = molecules.size
                molecules.add(Molecule(x,y+1,newGenome,_energy))
                return true
            }
        }
        if(y > 0){
            if(field[(x+FIELD_WIDTH-1)% FIELD_WIDTH][y-1] == -1){
                field[(x+FIELD_WIDTH-1)% FIELD_WIDTH][y-1] = molecules.size
                molecules.add(Molecule((x+FIELD_WIDTH-1)% FIELD_WIDTH,y-1,newGenome,_energy))
                return true
            }
        }
        if(y < FIELD_HEIGHT - 1){
            if(field[(x+1)% FIELD_WIDTH][y+1] == -1){
                field[(x+1)% FIELD_WIDTH][y+1] = molecules.size
                molecules.add(Molecule((x+1)% FIELD_WIDTH,y+1,newGenome,_energy))
                return true
            }
        }
        if(y < FIELD_HEIGHT - 1){
            if(field[(x+FIELD_WIDTH-1)% FIELD_WIDTH][y+1] == -1){
                field[(x+FIELD_WIDTH-1)% FIELD_WIDTH][y+1] = molecules.size
                molecules.add(Molecule((x+FIELD_WIDTH-1)% FIELD_WIDTH,y+1,newGenome,_energy))
                return true
            }
        }
        if(y > 0){
            if(field[(x+1)% FIELD_WIDTH][y-1] == -1){
                field[(x+1)% FIELD_WIDTH][y-1] = molecules.size
                molecules.add(Molecule((x+1)% FIELD_WIDTH,y-1,newGenome,_energy))
                return true
            }
        }
        return false
    }

    fun isFriendly(o: Molecule): Boolean{
        return (genome == o.genome)
    }

    fun move(pos: Int){ // return who was found
        var turn: Int = (direction+pos)%8
        var _x: Int = x
        var _y: Int = y
        if(turn == 0 || turn >= 6){
            _x = (_x-1+ FIELD_WIDTH)% FIELD_WIDTH
        }
        else if(turn in 2..4){
            _x = (_x+1)% FIELD_WIDTH
        }
        if(turn in 0..2){
            _y = Math.min(_y+1, FIELD_HEIGHT-1)
        }
        else if(turn in 4..6){
            _y = Math.max(_y-1, 0)
        }
        _x = (_x+ FIELD_WIDTH) % FIELD_WIDTH
        if(_y < 0 || _y > FIELD_HEIGHT){ // wall
            pointer = (pointer+genome.get((pointer+4)%GENOME_SIZE))% GENOME_SIZE
        }
        else if(field[_x][_y] == -1){ // empty
            field[_x][_y] = field[x][y]
            field[x][y] = -1
            x = _x
            y = _y
            pointer = (pointer+2)%GENOME_SIZE
        }
        else if(isFriendly(molecules.get(field[_x][_y]))){ // friendly
            pointer = (pointer+genome.get((pointer+2)%GENOME_SIZE))% GENOME_SIZE
        }
        else{ // unfriendly
            pointer = (pointer+genome.get((pointer+3)%GENOME_SIZE))% GENOME_SIZE
        }
    }

    fun turn(turn: Int){
        direction = (direction+turn)%8
        pointer = (pointer+1)%GENOME_SIZE
    }

    fun photosynthesis(){
        energy += fieldSun[x][y]
        pointer = (pointer+1)%GENOME_SIZE
    }

    fun eatOrganic(){
        if(fieldOrganic[x][y] > 0) {
            energy += 10
            --fieldOrganic[x][y]
        }
        pointer = (pointer+1)%GENOME_SIZE
    }

    fun eatEnemy(pos: Int){
        var turn: Int = (direction+pos)%8
        var _x: Int = x
        var _y: Int = y
        if(turn == 0 || turn >= 6){
            _x = (_x-1+ FIELD_WIDTH)% FIELD_WIDTH
        }
        else if(turn in 2..4){
            _x = (_x+1)% FIELD_WIDTH
        }
        if(turn in 0..2){
            _y = Math.min(_y+1, FIELD_HEIGHT-1)
        }
        else if(turn in 4..6){
            _y = Math.max(_y-1, 0)
        }
        _x = (_x+ FIELD_WIDTH) % FIELD_WIDTH
        if(_y < 0 || _y > FIELD_HEIGHT){ // wall
            pointer = (pointer+genome.get((pointer+4)%GENOME_SIZE))% GENOME_SIZE
        }
        else if(field[_x][_y] == -1){ // empty
            pointer = (pointer+genome.get((pointer+3)%GENOME_SIZE))% GENOME_SIZE
        }
        else if(isFriendly(molecules.get(field[_x][_y]))){ // friendly
//            System.err.println("HE IS FRIENDLY! " + _x + " " + _y + " | " + x + " " + y)
            pointer = (pointer+genome.get((pointer+2)%GENOME_SIZE))% GENOME_SIZE
        }
        else{ // unfriendly
//            System.err.println("I EAT HIM!")
            molecules.get(field[_x][_y]).isAlive = false
            ++killed
            energy += molecules.get(field[_x][_y]).energy
            ++fieldOrganic[_x][_y]
            pointer = (pointer+2)%GENOME_SIZE
        }
    }

}