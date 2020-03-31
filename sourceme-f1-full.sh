# you should source this only if you plan to run build/simulations locally,
# without using the manager at all.

unamestr=$(uname)
AWSFPGA=$RDIR/platforms/f1/aws-fpga
RDIR="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
AWSFPGA=$RDIR/platforms/f1/aws-fpga

# setup AWS tools
cd $AWSFPGA
source ./hdk_setup.sh
source ./sdk_setup.sh
export CL_DIR=$AWSFPGA/hdk/cl/developer_designs/cl_firesim
cd $RDIR

# setup risc-v tools
source ./env.sh

# put the manager on the user path
export PATH=$PATH:$RDIR/deploy

# setup ssh-agent
source deploy/ssh-setup.sh

# flag for scripts to check that this has been sourced
export FIRESIM_SOURCED=1
