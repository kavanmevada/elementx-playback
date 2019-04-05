# ElementX Music Playback Android Library

## Introduction
Android music playback functionality with latest codebase.

## Use Library
In the `MainActivity` class

```
    class MainActivity : MediaCompatActivity() {
	... }
```
thanks to it you will learn how to use the library.

*There are different ways of adding this library to your code*

### Gradle / Maven dependency
At the moment we do not have a publishing mechanism to a maven repository so the easiest way to add the library to your app is via a GitLab Dependency

```
repositories {
    ...
    maven { url "https://github.com/kavanmevada/elementx-playback/raw/master/maven" }
}
dependencies {
    ...
    implementation 'elementx.media:media:1.0.2'
}
```

### As a git submodule
Basically get this code and compile it having it integrated via a git submodule:

1. go into your own apps directory on the command line and add this lib as a submodule: ```git submodule add https://github.com/kavanmevada/elementx-playback```
2. Import/Open your app in Android Studio

##  License

```
    ElementX Music Playback (elementx-playback) 
    Copyright (C) 2019  Kavan Mevada

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
```