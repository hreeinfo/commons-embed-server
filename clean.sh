#!/bin/bash

dot_clean -m .
find . -name ".DS_Store" -depth -exec rm {} \;

