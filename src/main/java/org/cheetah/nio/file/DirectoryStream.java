/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cheetah.nio.file;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author phs
 */
public class DirectoryStream<T> implements Iterable<T> {
    
    @Override
    public Iterator<T> iterator(){
    
        return new ArrayList<T>().iterator();
    }

    
}
