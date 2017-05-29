package org.cheetah.webserver;


import java.util.Objects;

/*
 * Copyright 2016 phs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 * @author phs
 */

    public class FileInformation {

        private String name;
        private long size;
        private long lastModified;
        private boolean isDynamic;
        private boolean isPlugin;
        private boolean isFolder;

        public FileInformation(String name, long size, long lastModified, boolean isDynamic, boolean isPlugin, boolean isFolder) {

            this.name = name;
            this.size = size;
            this.lastModified = lastModified;
            this.isDynamic = isDynamic;
            this.isPlugin = isPlugin;
            this.isFolder = isFolder;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long lastModified) {
            this.lastModified = lastModified;
        }

        public boolean isIsDynamic() {
            return isDynamic;
        }

        public void setIsDynamic(boolean isDynamic) {
            this.isDynamic = isDynamic;
        }

        public boolean isIsPlugin() {
            return isPlugin;
        }

        public void setIsPlugin(boolean isPlugin) {
            this.isPlugin = isPlugin;
        }

        public boolean isIsFolder() {
            return isFolder;
        }

        public void setIsFolder(boolean isFolder) {
            this.isFolder = isFolder;
        }

        @Override
        public boolean equals(Object object) {

            if (object instanceof FileInformation) {

                FileInformation fileInformation = (FileInformation) object;
                return this.name.equals(fileInformation.name)
                        && this.size == fileInformation.size
                        && this.lastModified == fileInformation.lastModified
                        && this.isDynamic == fileInformation.isDynamic
                        && this.isPlugin == fileInformation.isPlugin;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + Objects.hashCode(this.name);
            hash = 89 * hash + (int) (this.size ^ (this.size >>> 32));
            hash = 89 * hash + (int) (this.lastModified ^ (this.lastModified >>> 32));
            hash = 89 * hash + (this.isDynamic ? 1 : 0);
            hash = 89 * hash + (this.isPlugin ? 1 : 0);
            return hash;
        }
    }
