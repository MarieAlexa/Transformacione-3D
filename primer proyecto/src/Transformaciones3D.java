<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8" />
  <title>Transformaciones 3D Interactivas - Javascript puro</title>
  <style>
    body { background: #222; color: #fff; font-family: Arial; }
    #ui-bar {
      margin-bottom: 15px;
      padding: 10px 0;
      text-align: center;
      background-color: #111;
    }
    select, input[type=number] { margin: 5px; padding: 3px; }
    input[type=button] { margin: 5px; padding: 5px 12px; background:#444;color:#fff;border:none;cursor:pointer;}
    input[type=button]:hover{background:#339;}
    canvas { background: #eee; border: 2px solid #333; display: block; margin: 0 auto; }
    #legend {text-align:center;}
  </style>
</head>
<body>

<div id="ui-bar">
  <b>Objeto:</b>
  <select id="object-select">
    <option value="pyramid">Pirámide</option>
    <option value="cube">Cubo</option>
    <option value="para">Paralelepípedo</option>
  </select>
  &nbsp; <b>Transformación:</b>
  <select id="transform-select">
    <option value="none">Ninguna</option>
    <option value="translate">Traslación</option>
    <option value="scale">Escalamiento</option>
    <option value="rotate">Rotación</option>
  </select>
  <span id="params"></span>
  <b>Proyección:</b>
  <select id="proj-select">
    <option value="ortho">glOrtho</option>
    <option value="frustum">glFrustum</option>
    <option value="persp">gluPerspective</option>
  </select>
  <input type="button" value="Aplicar" onclick="applyTransform()" />
  <input type="button" value="Reset" onclick="resetAll()" />
</div>
<div id="legend"></div>
<canvas id="canvas" width="700" height="520"></canvas>
<script>

const OBJS = {
  pyramid: {
    name: 'Pirámide',
    verts: [
      [1,0,-1], [3,0,-1], [3,0,-3], [1,0,-3], [2,2,-2]
    ],
    edges: [
      [0,1],[1,2],[2,3],[3,0], // base
      [0,4],[1,4],[2,4],[3,4]
    ],
    faces: [
      [0,1,2,3], [0,1,4], [1,2,4], [2,3,4], [3,0,4]
    ]
  },
  cube: {
    name: 'Cubo',
    verts: [
      [-4,2,-1],[-2,2,-1],[-2,2,-3],[-4,2,-3],
      [-4,4,-1],[-2,4,-1],[-2,4,-3],[-4,4,-3]
    ],
    edges: [
      [0,1],[1,2],[2,3],[3,0],
      [4,5],[5,6],[6,7],[7,4],
      [0,4],[1,5],[2,6],[3,7]
    ],
    faces: [
      [0,1,2,3], [4,5,6,7], [0,1,5,4], [3,2,6,7], [1,2,6,5], [0,3,7,4]
    ]
  },
  para: {
    name: 'Paralelepípedo',
    verts: [
      [-2,-2,5],[2,-2,5],[2,-2,2],[-2,-2,2],
      [-1,-1,4],[1,-1,4],[1,-1,3],[-1,-1,3]
    ],
    edges: [
      [0,1],[1,2],[2,3],[3,0],
      [4,5],[5,6],[6,7],[7,4],
      [0,4],[1,5],[2,6],[3,7]
    ],
    faces: [
      [0,1,2,3], [4,5,6,7], [0,1,5,4], [3,2,6,7], [1,2,6,5], [0,3,7,4]
    ]
  }
};



let active = {
  object: 'pyramid',
  verts: [],
  edges: [],
  faces: [],
  proj: 'ortho',
  transform: 'none',
 
  transMat: mat4identity()
};

const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d');
const objectSelect = document.getElementById('object-select');
const transformSelect = document.getElementById('transform-select');
const projSelect = document.getElementById('proj-select');
const paramsDiv = document.getElementById('params');




function mat4mul(mat, v) {
  let r = [0,0,0,0];
  for(let i=0;i<4;i++)
    for(let j=0;j<4;j++)
      r[i] += mat[i][j]*v[j];
  return r;
}
function mat4mulmat(a, b) {
  let r = [];
  for(let i=0;i<4;i++) {
    r[i] = [];
    for(let j=0;j<4;j++) {
      r[i][j] = 0;
      for(let k=0;k<4;k++) r[i][j] += a[i][k]*b[k][j];
    }
  }
  return r;
}
function mat4identity() {
  return [
    [1,0,0,0],
    [0,1,0,0],
    [0,0,1,0],
    [0,0,0,1]
  ];
}
{
  let m = mat4identity();
  m[0][3]=dx; m[1][3]=dy; m[2][3]=dz;
  return m;
}

function mat4scale(sx,sy,sz) {
  let m = mat4identity();
  m[0][0]=sx; m[1][1]=sy; m[2][2]=sz;
  return m;
}

function mat4rotateAxis(angleDeg, x, y, z) {
  let angle = angleDeg * Math.PI/180;
  let l = Math.sqrt(x*x+y*y+z*z); if(l===0){x=1;y=0;z=0;l=1;}
  x/=l; y/=l; z/=l;
  let c=Math.cos(angle), s=Math.sin(angle), t=1-c;
  return [
    [t*x*x + c,   t*x*y - z*s, t*x*z + y*s, 0],
    [t*x*y + z*s, t*y*y + c,   t*y*z - x*s, 0],
    [t*x*z - y*s, t*y*z + x*s, t*z*z + c,   0],
    [0,0,0,1]
  ];
}


function project3D([x,y,z], type, opt) {
  
  if(type==="ortho") {
    
    let {l,r,b,t,n,f} = opt;
    let xn = (x-(l+r)/2)/((r-l)/2);
    let yn = (y-(b+t)/2)/((t-b)/2);
    let zn = (z-(n+f)/2)/((f-n)/2);
    return [xn, yn, zn];
  }
  if(type==="frustum") {
   
    let {l,r,b,t,n,f} = opt;
    let xn = (2*n*x)/(r-l)-((r+l)/(r-l));
    let yn = (2*n*y)/(t-b)-((t+b)/(t-b));
    let zn = -(f+n)/(f-n)-2*f*n/(z*(f-n));
    return [xn, yn, zn];
  }
  if(type==="persp") {
    let {fov,a,n,f} = opt;
    let d = 1 / Math.tan(fov*Math.PI/360);
    // z proyectado
    let xn = d * x / (-z); // z negativo
    let yn = d * y / (-z) * (1/a);
    return [xn,yn,z];
  }
  return [x,y,z];
}


function applyMatToVerts(verts, mat) {
  return verts.map(([x,y,z]) => {
    let v4 = mat4mul(mat,[x,y,z,1]);
    return [v4[0],v4[1],v4[2]];
  });
}


function updateUIParams() {
  let html = "";
  let t = transformSelect.value;
  if(t==="translate") {
    html = `dx: <input type="number" id="dx" value="1" step="0.1" style="width:50px"/>
            dy: <input type="number" id="dy" value="1" step="0.1" style="width:50px"/>
            dz: <input type="number" id="dz" value="1" step="0.1" style="width:50px"/>`;
  } else if(t==="scale") {
    html = `sx: <input type="number" id="sx" value="1.2" step="0.1" style="width:50px"/>
            sy: <input type="number" id="sy" value="1.2" step="0.1" style="width:50px"/>
            sz: <input type="number" id="sz" value="1.2" step="0.1" style="width:50px"/>`;
  } else if(t==="rotate") {
    html = `Ángulo (°): <input type="number" id="ang" value="45" style="width:50px"/>
      Eje: <select id="axis">
        <option value="x">X</option>
        <option value="y">Y</option>
        <option value="z">Z</option>
        <option value="u">Custom</option>
      </select>
      <span id="ucustom"></span>`;
  }
  paramsDiv.innerHTML = html;
  
  if(t==="rotate") {
    document.getElementById('axis').onchange = function() {
      if(this.value=="u") {
        document.getElementById('ucustom').innerHTML =
        ` x1: <input id="ux" type="number" value="1" style="width:30px"/>
          y1: <input id="uy" type="number" value="0" style="width:30px"/>
          z1: <input id="uz" type="number" value="1" style="width:30px"/>`;
      } else {
        document.getElementById('ucustom').innerHTML="";
      }
    };
  }
}
transformSelect.onchange = updateUIParams;
window.onload = updateUIParams;



function applyTransform() {
 
  let objval = objectSelect.value;
  if(active.object !== objval) {
    active.transMat = mat4identity();
    active.object = objval;
  }
  let obj = OBJS[objval];
  let mat = mat4identity();
  let tr = transformSelect.value;
  if(tr==="translate") {
    let dx = parseFloat(document.getElementById('dx').value)||0;
    let dy = parseFloat(document.getElementById('dy').value)||0;
    let dz = parseFloat(document.getElementById('dz').value)||0;
    mat = mat4translate(dx,dy,dz);
    active.transMat = mat4mulmat(mat,active.transMat);
  } else if(tr==="scale") {
    let sx = parseFloat(document.getElementById('sx').value)||1;
    let sy = parseFloat(document.getElementById('sy').value)||1;
    let sz = parseFloat(document.getElementById('sz').value)||1;
    mat = mat4scale(sx,sy,sz);
    active.transMat = mat4mulmat(mat,active.transMat);
  } else if(tr==="rotate") {
    let ang = parseFloat(document.getElementById('ang').value)||0;
    let axis = document.getElementById('axis').value;
    let x=0,y=0,z=0;
    if(axis==="x") x=1;
    else if(axis==="y") y=1;
    else if(axis==="z") z=1;
    else if(axis==="u") {
      x = parseFloat(document.getElementById('ux').value)||0;
      y = parseFloat(document.getElementById('uy').value)||0;
      z = parseFloat(document.getElementById('uz').value)||0;
    }
    mat = mat4rotateAxis(ang,x,y,z);
    active.transMat = mat4mulmat(mat,active.transMat);
  }
  active.verts = applyMatToVerts(obj.verts, active.transMat);
  active.edges = obj.edges;
  active.faces = obj.faces;
  active.proj = projSelect.value;

  drawScene();
}


function resetAll() {
  let objval = objectSelect.value;
  active.transMat = mat4identity();
  active.object = objval;
  let obj = OBJS[objval];
  active.verts = obj.verts.slice();
  active.edges = obj.edges;
  active.faces = obj.faces;
  active.proj = projSelect.value;
  drawScene();
}


const projParams = {
  ortho:   {l:-5, r:5, b:-5, t:5, n:-20, f:20},
  frustum: {l:-2, r:2, b:-2, t:2, n:2, f:22},
  persp:   {fov:60, a:700/520, n:2, f:40}
};

let camRotY = 30, camRotX = -20;

function drawScene() {
  ctx.clearRect(0,0,canvas.width,canvas.height);
 
  let pd = projParams[active.proj];
  let verts = active.verts || OBJS[active.object].verts;
  
  let camMatY = mat4rotateAxis(camRotY,0,1,0); 
  let camMatX = mat4rotateAxis(camRotX,1,0,0); 
  let camMat = mat4mulmat(camMatX,camMatY);
  let viewVerts = applyMatToVerts(verts, camMat);

  
  let points2D = [];
  for(let v of viewVerts) {
    let prj = project3D(v, active.proj, pd);
    let [x,y] = prj;
   
    x = canvas.width/2 + x*80;
    y = canvas.height/2 - y*80;
    points2D.push([x,y]);
  }

  
  if(active.faces){
    ctx.globalAlpha = 0.8;
    for(let i=0;i<active.faces.length;i++){
      let f = active.faces[i];
      ctx.beginPath();
      ctx.moveTo(points2D[f[0]][0], points2D[f[0]][1]);
      for(let j=1;j<f.length;j++) {
        ctx.lineTo(points2D[f[j]][0], points2D[f[j]][1]);
      }
      ctx.closePath();
      ctx.fillStyle = ['#fdd','#cfc','#ddf','#ccf','#eef'][i%5] || "#fec";
      ctx.fill();
      ctx.strokeStyle = '#000'; ctx.globalAlpha = 1; ctx.stroke();
    }
  }


  ctx.lineWidth=2; ctx.globalAlpha=1;
  for(let [a,b] of active.edges) {
    ctx.beginPath();
    ctx.moveTo(points2D[a][0],points2D[a][1]);
    ctx.lineTo(points2D[b][0],points2D[b][1]);
    ctx.strokeStyle="#222"; ctx.stroke();
  }

  ctx.font = "bold 15px Arial";
  for(let i=0;i<points2D.length;i++) {
    let [x,y] = points2D[i];
    ctx.beginPath();
    ctx.arc(x,y,8,0,2*Math.PI);
    ctx.fillStyle="#333"; ctx.fill();
    ctx.beginPath();
    ctx.arc(x,y,5,0,2*Math.PI);
    ctx.fillStyle="#fff"; ctx.fill();
    ctx.fillStyle="#039";
    ctx.textAlign="center";
    ctx.textBaseline="middle";
    ctx.fillText((i+1), x, y);
  }
  
  const vertsHTML = viewVerts.map( (v,i) =>
    `<b>V${i+1}</b>=[${v.map(a=>a.toFixed(2)).join(", ")}]`
  ).join(" &nbsp; ");
  document.getElementById("legend").innerHTML = 
    `<span style="font-size:1.1em">Vértices proyectados: </span>${vertsHTML}`;
}


let dragging = false, lastX=0, lastY=0;
canvas.onmousedown = function(e){
  dragging=true; lastX=e.offsetX; lastY=e.offsetY;
};
canvas.onmouseup = canvas.onmouseleave = ()=>dragging=false;
canvas.onmousemove = function(e){
  if(!dragging) return;
  camRotY += (e.offsetX-lastX)*0.6;
  camRotX += (e.offsetY-lastY)*0.6;
  lastX = e.offsetX; lastY = e.offsetY;
  drawScene();
};


objectSelect.onchange = resetAll;
projSelect.onchange = drawScene;
resetAll();

</script>
</body>
</html>








